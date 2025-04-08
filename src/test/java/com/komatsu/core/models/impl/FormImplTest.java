package com.komatsu.core.models.impl;

import com.komatsu.core.models.Form;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FormImplTest {

    private final AemContext aemContext=new AemContext();
    private Form form;

    @BeforeEach
    void setUp() {
        aemContext.addModelsForClasses(FormImpl.class);
        // still need to create Page.json object under resources folder
        aemContext.load().json("/Page.json","/page");
    }

    @Test
    void getResultsTest() {
        form=aemContext.request().adaptTo(Form.class);
        assertEquals("Page Title",form.getPageTitle());
    }

}