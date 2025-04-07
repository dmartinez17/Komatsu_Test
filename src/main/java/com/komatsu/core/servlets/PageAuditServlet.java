package com.komatsu.core.servlets;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.framework.Constants;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "= Page Author Audit Servlet",
                "sling.servlet.paths=/bin/page-author-audit",
                "sling.servlet.extensions=json",
                "sling.servlet.extensions=xml",
                "sling.servlet.methods=GET"
        }
)
public class PageAuditServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String pagePath = request.getParameter("path");

        if (pagePath == null || pagePath.isEmpty()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing 'path' parameter");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page page = pageManager.getPage(pagePath);

        if (page == null) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Page not found: " + pagePath);
            return;
        }

        String lastModifiedBy = page.getProperties().get("cq:lastModifiedBy", "");
        String firstName = resolver.getUserManager().getAuthorizable(lastModifiedBy)
                .getProperty("profile/givenName")[0].getString();
        String lastName = resolver.getUserManager().getAuthorizable(lastModifiedBy)
                .getProperty("profile/familyName")[0].getString();

        List<String> modifiedChildPages = new ArrayList<>();
        Iterator<Page> children = page.listChildren();
        while (children.hasNext()) {
            Page child = children.next();
            String childModifiedBy = child.getProperties().get("cq:lastModifiedBy", "");
            if (lastModifiedBy.equals(childModifiedBy)) {
                modifiedChildPages.add(child.getPath());
            }
        }

        String ext = request.getRequestPathInfo().getExtension();
        if ("json".equals(ext)) {
            response.setContentType("application/json");
            JSONObject output = new JSONObject();
            output.put("firstName", firstName);
            output.put("lastName", lastName);
            output.put("modifiedChildPages", new JSONArray(modifiedChildPages));
            response.getWriter().write(output.toString(2));
        } else if ("xml".equals(ext)) {
            response.setContentType("application/xml");
            response.getWriter().write("<author><firstName>" + firstName +
                    "</firstName><lastName>" + lastName + "</lastName><modifiedChildPages>");
            for (String path : modifiedChildPages) {
                response.getWriter().write("<page>" + path + "</page>");
            }
            response.getWriter().write("</modifiedChildPages></author>");
        } else {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Unsupported extension");
        }
    }
}


