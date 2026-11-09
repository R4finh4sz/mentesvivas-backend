package com.mentesvivas.backend.support;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class ClearTemporaryStorage implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        SpringExtension.getApplicationContext(context)
                .getBean(InMemoryTemporaryStorage.class)
                .clear();
    }
}
