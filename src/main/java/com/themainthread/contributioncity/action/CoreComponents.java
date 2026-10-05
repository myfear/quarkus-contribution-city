package com.themainthread.contributioncity.action;

import com.themainthread.contributioncity.city.CityBuilder;
import com.themainthread.contributioncity.render.SvgRenderer;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class CoreComponents {
    @Produces
    @Singleton
    CityBuilder cityBuilder() {
        return new CityBuilder();
    }

    @Produces
    @Singleton
    SvgRenderer svgRenderer() {
        return new SvgRenderer();
    }
}
