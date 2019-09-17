/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import example.models.ArtifactGroup;
import example.models.ArtifactProduct;
import example.models.ArtifactVersion;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import org.glassfish.hk2.api.ServiceLocator;

import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

/**
 * This class contains common settings for both test and production.
 */
public abstract class CommonElideSettings implements ElideStandaloneSettings {

    @Override
    public int getPort() {
        //Heroku exports port to come from $PORT
        return Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::valueOf)
                .orElse(4080);
    }

    @Override
    public Map<String, Swagger> enableSwagger() {
        EntityDictionary dictionary = new EntityDictionary(new HashMap());

        dictionary.bindEntity(ArtifactGroup.class);
        dictionary.bindEntity(ArtifactProduct.class);
        dictionary.bindEntity(ArtifactVersion.class);
        Info info = new Info().title("Test Service").version("1.0");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        Swagger swagger = builder.build();

        Map<String, Swagger> docs = new HashMap<>();
        docs.put("test", swagger);
        return docs;
    }

    @Override
    public String getModelPackageName() {

        //This needs to be changed to the package where your models live.
        return "example.models";
    }

    @Override
    public ElideSettings getElideSettings(ServiceLocator injector) {
        EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(this.getModelPackageName(),
                getDatabaseProperties());
        DataStore dataStore = new JpaDataStore(() -> {
            return entityManagerFactory.createEntityManager();
        }, (em) -> {
            return new NonJtaTransaction(em);
        });
        Map var10002 = this.getCheckMappings();
        injector.getClass();
        EntityDictionary dictionary = new EntityDictionary(var10002, injector::inject);
        ElideSettingsBuilder builder = (new ElideSettingsBuilder(dataStore))
                .withUseFilterExpressions(true)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary));
        if (this.enableIS06081Dates()) {
            builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        }

        return builder.build();
    }

    public abstract Properties getDatabaseProperties();
}