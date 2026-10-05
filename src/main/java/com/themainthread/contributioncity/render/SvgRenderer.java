package com.themainthread.contributioncity.render;

import java.util.ArrayList;
import java.util.List;

import com.themainthread.contributioncity.city.Cell;
import com.themainthread.contributioncity.city.CellKind;
import com.themainthread.contributioncity.city.Scene;

public class SvgRenderer {
    private static final int CELL_WIDTH = 10;
    private static final int CELL_HEIGHT = 18;
    private static final int PAD = 16;

    public String render(Scene scene, Theme theme) {
        int contentWidth = scene.width() * CELL_WIDTH;
        int width = contentWidth + PAD * 2;
        List<String> header = new ArrayList<>();
        int headerColumns = Math.max(1, contentWidth / 8);
        header.addAll(wrap("@" + scene.username(), headerColumns));
        String total = scene.totalContributions() + " contributions";
        header.addAll(wrap(total.length() <= headerColumns ? total : scene.totalContributions() + " total", headerColumns));
        int headerHeight = Math.max(44, 12 + header.size() * CELL_HEIGHT);
        int height = headerHeight + scene.rows().size() * CELL_HEIGHT + 12;
        String title = "GitHub contribution city for @" + scene.username() + ": "
                + scene.totalContributions() + " contributions";
        StringBuilder svg = new StringBuilder(8192);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
                .append(width).append(' ').append(height)
                .append("\" role=\"img\" aria-labelledby=\"title desc\">\n")
                .append("<title id=\"title\">").append(escape(title)).append("</title>\n")
                .append("<desc id=\"desc\">Oldest weeks are on the left. Building height uses logarithmic weekly activity. ")
                .append("Windows repeat each week's returned days, colored by contribution level. ")
                .append("The antenna marks the most recent highest active week. ")
                .append("The header total covers the full calendar returned by GitHub.</desc>\n")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"").append(theme.background()).append("\"/>\n")
                .append("<g font-family=\"ui-monospace, 'SF Mono', Menlo, Consolas, 'DejaVu Sans Mono', monospace\" ")
                .append("font-size=\"13\" fill=\"").append(theme.label()).append("\">\n");
        for (int i = 0; i < header.size(); i++) {
            svg.append("<text x=\"").append(PAD).append("\" y=\"").append(22 + i * CELL_HEIGHT)
                    .append("\">").append(escape(header.get(i))).append("</text>\n");
        }
        svg.append("</g>\n");
        drawFacades(svg, scene, theme, headerHeight);
        svg.append("<g font-family=\"ui-monospace, 'SF Mono', Menlo, Consolas, 'DejaVu Sans Mono', monospace\" ")
                .append("font-size=\"15\" text-anchor=\"middle\">\n");
        for (int rowIndex = 0; rowIndex < scene.rows().size(); rowIndex++) {
            List<Cell> row = scene.rows().get(rowIndex);
            int column = 0;
            while (column < row.size()) {
                Cell cell = row.get(column);
                if (cell.kind() == CellKind.SKY) {
                    column++;
                    continue;
                }
                String color = color(cell, theme);
                StringBuilder positions = new StringBuilder();
                StringBuilder glyphs = new StringBuilder();
                do {
                    if (!positions.isEmpty()) {
                        positions.append(' ');
                    }
                    positions.append(PAD + column * CELL_WIDTH + CELL_WIDTH / 2);
                    glyphs.append(row.get(column).glyph());
                    column++;
                } while (column < row.size() && row.get(column).kind() != CellKind.SKY
                        && color.equals(color(row.get(column), theme)));
                svg.append("<text x=\"").append(positions).append("\" y=\"")
                        .append(headerHeight + rowIndex * CELL_HEIGHT + 14)
                        .append("\" fill=\"").append(color).append("\">")
                        .append(escape(glyphs.toString())).append("</text>\n");
            }
        }
        return svg.append("</g>\n</svg>\n").toString();
    }

    private void drawFacades(StringBuilder svg, Scene scene, Theme theme, int headerHeight) {
        for (int row = 0; row < scene.rows().size(); row++) {
            int column = 0;
            while (column < scene.width()) {
                Cell cell = scene.rows().get(row).get(column);
                if (cell.kind() != CellKind.WINDOW) {
                    column++;
                    continue;
                }
                int start = column;
                while (column < scene.width() && sameFacade(scene.rows().get(row).get(column), cell)) {
                    column++;
                }
                if (row > 0 && sameFacade(scene.rows().get(row - 1).get(start), cell)) {
                    continue;
                }
                int end = row + 1;
                while (end < scene.rows().size() && sameFacade(scene.rows().get(end).get(start), cell)) {
                    end++;
                }
                svg.append("<rect x=\"").append(PAD + start * CELL_WIDTH + 1).append("\" y=\"")
                        .append(headerHeight + row * CELL_HEIGHT).append("\" width=\"")
                        .append((column - start) * CELL_WIDTH - 2).append("\" height=\"")
                        .append((end - row) * CELL_HEIGHT).append("\" fill=\"")
                        .append(cell.buildingShade() == 0 ? theme.buildingA() : theme.buildingB()).append("\"/>\n");
            }
        }
    }

    private boolean sameFacade(Cell cell, Cell other) {
        return cell.kind() == CellKind.WINDOW && cell.buildingShade() == other.buildingShade();
    }

    private String color(Cell cell, Theme theme) {
        return switch (cell.kind()) {
            case WINDOW -> theme.windows().get(cell.intensity());
            case ROOF, ANTENNA -> theme.roof();
            case STREET -> theme.street();
            case LABEL, SKY -> theme.label();
        };
    }

    private List<String> wrap(String text, int columns) {
        List<String> lines = new ArrayList<>();
        while (text.length() > columns) {
            int space = text.lastIndexOf(' ', columns);
            int hyphen = text.lastIndexOf('-', columns - 1);
            int end = space > 0 ? space : hyphen > 0 ? hyphen + 1 : columns;
            lines.add(text.substring(0, end));
            text = text.substring(end).stripLeading();
        }
        lines.add(text);
        return lines;
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
