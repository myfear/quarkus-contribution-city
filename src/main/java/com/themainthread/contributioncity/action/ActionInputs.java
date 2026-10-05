package com.themainthread.contributioncity.action;

import com.themainthread.contributioncity.city.CityOptions;
import com.themainthread.contributioncity.render.Themes;

import io.quarkiverse.githubaction.Inputs;

public record ActionInputs(String username, CityOptions options) {
    public static ActionInputs read(Inputs inputs) {
        String username = inputs.get("username").orElse("").trim();
        if (!username.matches("[A-Za-z0-9-]{1,39}")) {
            throw new IllegalArgumentException("username must contain 1..39 letters, digits, or hyphens");
        }
        CityOptions options = new CityOptions(integer(inputs, "weeks", "53"), integer(inputs, "height", "14"),
                Themes.named(inputs.get("theme").orElse("github-dark").trim()));
        if (inputs.get("github-token").orElse("").isBlank()) {
            throw new IllegalArgumentException("github-token is required");
        }
        return new ActionInputs(username, options);
    }

    private static int integer(Inputs inputs, String name, String fallback) {
        try {
            return Integer.parseInt(inputs.get(name).orElse(fallback).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }
}
