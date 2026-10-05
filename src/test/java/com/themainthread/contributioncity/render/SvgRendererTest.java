package com.themainthread.contributioncity.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.themainthread.contributioncity.Fixtures;
import com.themainthread.contributioncity.city.Cell;
import com.themainthread.contributioncity.city.CellKind;
import com.themainthread.contributioncity.city.CityBuilder;
import com.themainthread.contributioncity.city.CityOptions;
import com.themainthread.contributioncity.city.Scene;

class SvgRendererTest {
    private final SvgRenderer renderer = new SvgRenderer();

    @ParameterizedTest
    @ValueSource(strings = { "github-dark", "mono" })
    void producesDeterministicStandaloneXml(String themeName) throws Exception {
        Theme theme = Themes.named(themeName);
        Scene scene = new CityBuilder().build(Fixtures.calendar(0, 3, 20), new CityOptions(4, 14, theme));
        String svg = renderer.render(scene, theme);
        assertEquals(svg, renderer.render(scene, theme));
        Document document = parse(svg);
        assertEquals("svg", document.getDocumentElement().getLocalName());
        assertEquals("http://www.w3.org/2000/svg", document.getDocumentElement().getNamespaceURI());
        assertTrue(document.getElementsByTagName("title").item(0).getTextContent().contains("23 contributions"));
        assertFalse(svg.contains("<script"));
        assertFalse(svg.contains("foreignObject"));
        assertFalse(svg.contains("href="));
        assertFalse(svg.contains("<animate"));
        assertTrue(svg.contains("monospace"));
        assertEquals("0 0 92 420", document.getDocumentElement().getAttribute("viewBox"));
    }

    @Test
    void escapesHeaderTitleAndCellText() throws Exception {
        String dangerous = "&<>\"'";
        Scene scene = new Scene(dangerous, 9, List.of(List.of(new Cell('<', CellKind.LABEL, 0, -1),
                new Cell('&', CellKind.LABEL, 0, -1))));
        String svg = renderer.render(scene, Themes.MONO);
        assertTrue(svg.contains("&amp;"));
        assertTrue(svg.contains("&lt;"));
        assertTrue(svg.contains("&gt;"));
        assertTrue(svg.contains("&quot;"));
        assertTrue(svg.contains("&apos;"));
        assertEquals("GitHub contribution city for @" + dangerous + ": 9 contributions",
                parse(svg).getElementsByTagName("title").item(0).getTextContent());
    }

    @Test
    void shortImagesWrapLongHeadersAndKeepEveryGlyphInsideViewBox() throws Exception {
        Scene original = new CityBuilder().build(Fixtures.calendar(1, 2, 3, 4), new CityOptions(4, 3, Themes.MONO));
        Scene scene = new Scene("a".repeat(39), Integer.MAX_VALUE, original.rows());
        Document document = parse(renderer.render(scene, Themes.MONO));
        int width = Integer.parseInt(document.getDocumentElement().getAttribute("width"));
        int height = Integer.parseInt(document.getDocumentElement().getAttribute("height"));
        assertEquals(112, width);
        var nodes = document.getElementsByTagName("text");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element text = (Element) nodes.item(i);
            int y = Integer.parseInt(text.getAttribute("y"));
            assertTrue(y > 0 && y < height);
            for (String x : text.getAttribute("x").split(" ")) {
                assertTrue(Integer.parseInt(x) >= 16 && Integer.parseInt(x) < width - 16);
            }
            if (!text.hasAttribute("fill")) {
                assertTrue(text.getTextContent().length() <= 10);
            }
        }
    }

    @Test
    void drawsOneInsetFacadePerBuildingInsteadOfPerCell() throws Exception {
        Scene scene = new CityBuilder().build(Fixtures.calendar(0, 3, 20), new CityOptions(4, 14, Themes.MONO));
        Document document = parse(renderer.render(scene, Themes.MONO));
        var rects = document.getElementsByTagName("rect");
        assertEquals(4, rects.getLength());
        for (int i = 1; i < rects.getLength(); i++) {
            assertEquals("18", ((Element) rects.item(i)).getAttribute("width"));
        }
    }

    @Test
    void rejectsCssInjectionAndUnknownThemes() {
        assertThrows(IllegalArgumentException.class, () -> Themes.named("matrix2"));
        assertThrows(IllegalArgumentException.class, () -> new Theme("bad", "url(https://example.com)",
                "#000000", "#000000", "#000000", "#000000", "#000000", Themes.MONO.windows()));
    }

    private Document parse(String svg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
    }
}
