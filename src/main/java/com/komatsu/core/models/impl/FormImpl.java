package com.komatsu.core.models.impl;

import com.komatsu.core.models.Form;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

@Model(adaptables = slingHttpServletRequest.class,
        adapters = Form.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class FormImpl implements Form {
    private static final Logger LOG = LoggerFactory.getLogger(FormImpl.class);

    // parent page from where to check all search results
    final HOME_PAGE_PATH = "/content/komatsu/us/en/home"

    @SlingObject
    ResourceResolver resourceResolver;

    @ValueMapValue
    private String search;

    PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

    @Override
    public List<Map<String, String>> getResults() {

        List<Map<String, String>> searchResults = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("path", HOME_PAGE_PATH);
        map.put("type", "cq:Page");
        map.put("group.p.or", "true");
        map.put("group.1_fulltext", search);
        map.put("group.1_fulltext.relPath", "jcr:description");
        map.put("group.2_fulltext", search);
        map.put("group.2_fulltext.relPath", "jcr:title");
        Query query = builder.createQuery(PredicateGroup.create(map), session);
        SearchResult result = query.getResult();

        if (result.getTotalMatches() == 0) {
            return;
        }
        Calendar cal = new GregorianCalendar();
        String date = ;

        for (Hit hit : result.getHits()) {
            String path = hit.getPath();
            Page page = pageManager.getPage(path);
            Map<String,String> singleResult=new HashMap<>();
            // almost pseudo code, some of these page properties values have different methods
            singleResult.put("title",page.getProperties().get("cq:title", ""));
            singleResult.put("description",page.getProperties().get("cq:description", ""));
            singleResult.put("image",page.getProperties().get("cq:image", ""));
            // this is for sure not correct. the specific method would depend on the specific required format of the date
            singleResult.put("date",page.getLastModified().toString);
            searchResults.add(singleResult);
        }

        return searchResults;
    }

}
