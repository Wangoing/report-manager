package org.acme.tika;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jboss.logging.Logger;

import io.quarkus.tika.TikaParser;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path("/parse")
public class TikaParserResource {
    private static final Logger log = Logger.getLogger(TikaParserResource.class);

    private List<Person> persons = new ArrayList<>();

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("[\\d]{11}");

    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("([1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx])|([1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3})");

    @Inject
    TikaParser parser;

    @POST
    @Path("/text")
    @Consumes({"application/pdf", "application/vnd.oasis.opendocument.text"})
    @Produces(MediaType.TEXT_PLAIN)
    public String extractText(InputStream stream) {
        Instant start = Instant.now();

        String text = parser.getText(stream);

        Instant finish = Instant.now();

        log.info(Duration.between(start, finish).toMillis() + " mls have passed");

        return text;
    }

    @GET
    @Path("/infos")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> infos() {
        return persons;
    }

    @POST
    @Path("/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String uploadFile(@MultipartForm FormData formData) throws IOException {
        PDDocument pddDocument = PDDocument.load(formData.data);
        PDFTextStripper textStripper = new PDFTextStripper();
        String result = textStripper.getText(pddDocument);
        Person person = new Person();

        int nameIndex = result.indexOf("姓");
        int sexIndex = result.indexOf("别：");

        String name = result.substring(sexIndex + 2, nameIndex);
        System.out.println(name);
        person.setName(name);
        String phoneRegex = "[\\d]{11}";
        Pattern p = Pattern.compile(phoneRegex);
        Matcher m = PHONE_PATTERN.matcher(result);
        if (m.find()) {
            person.setPhone(m.group());
        }

        String idRegex = "([1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx])|([1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3})";

        p = Pattern.compile(idRegex);
        m = ID_CARD_PATTERN.matcher(result);
        if (m.find()) {
            person.setIdCard(m.group());
        }


        persons.add(person);
        return "OK";
    }
}
