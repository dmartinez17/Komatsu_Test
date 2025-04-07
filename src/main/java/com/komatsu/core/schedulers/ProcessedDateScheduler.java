package com.komatsu.core.schedulers;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

@Component(service = Runnable.class, immediate = true, configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
@Designate(ocd = ProcessedDateScheduler.CronConfig.class)
public class ProcessedDateScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessedDateScheduler.class);

    @ObjectClassDefinition(name = "Processed Date Scheduler", description = "Scheduled task to update processedDate on published pages")
    public @interface CronConfig {
        String cronExpression() default "0 0/2 * * * ?"; // every 2 minutes
        boolean authorOnly() default true;
    }

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private CronConfig cronConfig;

    @Activate
    @Modified
    protected void activate(CronConfig cronConfig) {
        this.cronConfig = cronConfig;
        ScheduleOptions options = scheduler.EXPR(cronConfig.cronExpression());
        options.name(this.getClass().getSimpleName());
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);
    }

    @Override
    public void run() {

        LOG.info("\n SCHEDULER RUN METHOD STARTED");

        if (!cronConfig.authorOnly()) {
            return;
        }

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(Map.of(ResourceResolverFactory.SUBSERVICE, "data-writer"))) {
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) return;

            Iterator<Page> pages = pageManager.getPage("/content/komatsu").listChildren(null, true);
            while (pages.hasNext()) {
                Page page = pages.next();
                if (!page.isValid() || !page.isPublished()) continue;

                Resource resource = page.adaptTo(Resource.class);
                if (resource != null && resource.getResourceType().equals("cq:Page")) {
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        node.setProperty("processedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()));
                    }
                }
            }
            resolver.commit();
        } catch (Exception e) {
            LOG.error("Error updating processedDate on published pages", e);
        }
    }
}


