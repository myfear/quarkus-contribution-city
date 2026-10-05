package com.themainthread.contributioncity.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.themainthread.contributioncity.city.CityBuilder;
import com.themainthread.contributioncity.city.Scene;
import com.themainthread.contributioncity.github.GitHubContributionClient;
import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.render.SvgRenderer;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;

public class ContributionCityAction {
    private final GitHubContributionClient contributionClient;
    private final CityBuilder cityBuilder;
    private final SvgRenderer svgRenderer;

    public ContributionCityAction(GitHubContributionClient contributionClient, CityBuilder cityBuilder,
            SvgRenderer svgRenderer) {
        this.contributionClient = contributionClient;
        this.cityBuilder = cityBuilder;
        this.svgRenderer = svgRenderer;
    }

    @Action
    void generate(Inputs inputs, Commands commands, Context context) throws IOException {
        ActionInputs configuration = ActionInputs.read(inputs);
        ContributionCalendar calendar = contributionClient.fetch(configuration.username(),
                inputs.getGitHubToken().orElseThrow(), context.getGithubGraphQLUrl());
        Scene scene = cityBuilder.build(calendar, configuration.options());
        String svg = svgRenderer.render(scene, configuration.options().theme());
        Files.writeString(Path.of("contribution-city.svg"), svg, StandardCharsets.UTF_8);
        commands.setOutput("svg-path", "contribution-city.svg");
        commands.setOutput("total-contributions", Integer.toString(calendar.totalContributions()));
        commands.appendJobSummary("## Contribution City\n\nGenerated `contribution-city.svg` for `@"
                + calendar.username() + "`.\n\n- " + calendar.totalContributions() + " contributions in the full calendar\n- "
                + Math.min(configuration.options().weeks(), calendar.weeks().size()) + " weeks rendered\n- Theme: `"
                + configuration.options().theme().name() + "`\n");
    }
}
