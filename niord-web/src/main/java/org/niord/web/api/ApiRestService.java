/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.niord.core.NiordApp;
import org.niord.core.area.Area;
import org.niord.core.message.Message;
import org.niord.core.publication.Publication;
import org.niord.model.DataFilter;
import org.niord.model.message.AreaVo;
import org.niord.model.message.MainType;
import org.niord.model.message.MessageVo;
import org.niord.model.publication.PublicationVo;
import org.niord.model.search.PagedSearchResultVo;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A public REST API for accessing message and publications Niord data.
 * <p>
 * Also, defines the Swagger API using annotations.
 * <p>
 * NB: Swagger codegen-generated model classes cannot handle UNIX Epoch timestamps, which is the date format
 * used in JSON throughout Niord. Instead, these classes expects dates to be encoded as ISO-8601 strings
 * (e.g. "2016-10-12T13:21:22.000+0000").<br>
 * To facilitate both formats, use the "dateFormat" parameter.
 */
@Api(value = "/public/v1",
     description = "Public API for accessing message and publication data from the Niord NW-NM system",
     tags = {"messages", "publications" })
@Path("/public/v1")
@Stateless
@SuppressWarnings("unused")
public class ApiRestService extends AbstractApiService {

    /**
     * The format to use for dates in generated JSON
     **/
    public enum JsonDateFormat {
        UNIX_EPOCH, ISO_8601
    }

    @Inject
    NiordApp app;


    /***************************
     * Message end-points
     ***************************/


    /**
     * {@inheritDoc}
     */
    @ApiOperation(
            value = "Returns the published NW and NM messages",
            response = MessageVo.class,
            responseContainer = "List",
            tags = {"messages"}
    )
    @GET
    @Path("/messages")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response searchMessages(
            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language,

            @ApiParam(value = "The IDs of the domains to select messages from", example = "niord-client-nw")
            @QueryParam("domain") Set<String> domainIds,

            @ApiParam(value = "Specific message series to select messages from", example = "dma-nw")
            @QueryParam("messageSeries") Set<String> messageSeries,

            @ApiParam(value = "The IDs of the publications to select message from")
            @QueryParam("publication") Set<String> publicationIds,

            @ApiParam(value = "The IDs of the areas to select messages from", example = "urn:mrn:iho:country:dk")
            @QueryParam("areaId") Set<String> areaIds,

            @ApiParam(value = "Either NW (navigational warnings) or NM (notices to mariners)", example = "NW")
            @QueryParam("mainType") Set<MainType> mainTypes,

            @ApiParam(value = "Well-Known Text for geographical extent", example = "POLYGON((7 54, 7 57, 13 56, 13 57, 7 54))")
            @QueryParam("wkt") String wkt,

            @ApiParam(value = "Whether to rewrite all embedded links and paths to be absolute URL's", example = "true")
            @QueryParam("externalize") @DefaultValue("true") boolean externalize,

            @ApiParam(value = "The date format to use for JSON date-time encoding. Either 'UNIX_EPOCH' or 'ISO_8601'", example = "UNIX_EPOCH")
            @QueryParam("dateFormat") @DefaultValue("UNIX_EPOCH") JsonDateFormat dateFormat

    ) throws Exception {

        // Perform the search
        PagedSearchResultVo<Message> searchResult =
                super.searchMessages(language, domainIds, messageSeries, publicationIds, areaIds, mainTypes, wkt);


        // Convert messages to value objects and externalize message links, if requested
        List<MessageVo> messages = searchResult
                .map(m -> toMessageVo(m, language, externalize))
                .getData();

        // Depending on the dateFormat param, either use UNIX epoch or ISO-8601
        StreamingOutput stream = os -> objectMapperForDateFormat(dateFormat).writeValue(os, messages);

        return Response
                .ok(stream, MediaType.APPLICATION_JSON_TYPE)
                .build();

    }


    /**
     * {@inheritDoc}
     */
    @ApiOperation(
            value = "Returns the public (published, cancelled or expired) NW or NM message details",
            response = MessageVo.class,
            tags = {"messages"}
    )
    @GET
    @Path("/message/{messageId}")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response messageDetails(
            @ApiParam(value = "The message UID or short ID", example = "NM-1275-16")
            @PathParam("messageId") String messageId,

            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language,

            @ApiParam(value = "Whether to rewrite all embedded links and paths to be absolute URL's", example = "true")
            @QueryParam("externalize") @DefaultValue("true") boolean externalize,

            @ApiParam(value = "The date format to use for JSON date-time encoding. Either 'UNIX_EPOCH' or 'ISO_8601'", example = "UNIX_EPOCH")
            @QueryParam("dateFormat") @DefaultValue("UNIX_EPOCH") JsonDateFormat dateFormat

    ) throws Exception {

        // Perform the search
        Message message = super.getMessage(messageId);

        if (message == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("No message found with ID: " + messageId)
                    .build();
        } else {

            // Convert message to value objects and externalize message links, if requested
            MessageVo result = toMessageVo(message, language, externalize);

            // Depending on the dateFormat param, either use UNIX epoch or ISO-8601
            StreamingOutput stream = os -> objectMapperForDateFormat(dateFormat).writeValue(os, result);

            return Response
                    .ok(stream, MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }



    /**
     * Returns the XSD for the Message class.
     * Two XSDs are produced, "schema1.xsd" and "schema2.xsd". The latter is the main schema for
     * Message and will import the former.
     * @return the XSD for the Message class
     */
    @ApiOperation(
            value = "Returns XSD model of the Message class",
            tags = {"messages"}
    )
    @GET
    @Path("/xsd/{schemaFile}")
    @Produces("application/xml;charset=UTF-8")
    @GZIP
    @NoCache
    public String getMessageXsd(
            @ApiParam(value = "The schema file, either schema1.xsd or schema2.xsd", example="schema2.xsd")
            @PathParam("schemaFile")
                    String schemaFile) throws Exception {

        if (!schemaFile.equals("schema1.xsd") && !schemaFile.equals("schema2.xsd")) {
            throw new Exception("Only 'schema1.xsd' and 'schema2.xsd' allowed");
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(MessageVo.class);

        Map<String, StringWriter> result = new HashMap<>();
        SchemaOutputResolver sor = new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
                StringWriter out = new StringWriter();
                result.put(suggestedFileName, out);
                StreamResult result = new StreamResult(out);
                result.setSystemId(app.getBaseUri() + "/rest/public/v1/xsd/" + suggestedFileName);
                return result;
            }

        };

        // Generate the schemas
        jaxbContext.generateSchema(sor);

        return result.get(schemaFile).toString();
    }


    /**
     * Convert the message to a value object representation.
     * If requested, rewrite all links to make them external URLs.
     * @param msg the message to convert to a value object
     * @param externalize whether to rewrite all links to make them external URLs
     * @return the message value object representation
     **/
    private MessageVo toMessageVo(Message msg, String language, boolean externalize) {

        // Sanity check
        if (msg == null) {
            return null;
        }

        // Convert the message to a value object
        DataFilter filter = Message.MESSAGE_DETAILS_FILTER.lang(language);
        MessageVo message = msg.toVo(MessageVo.class, filter);

        // If "externalize" is set, rewrite all links to make them external
        if (externalize) {
            String baseUri = app.getBaseUri();

            if (message.getParts() != null) {
                String from = concat("/rest/repo/file", msg.getRepoPath());
                String to = concat(baseUri, from);
                message.getParts().forEach(mp -> mp.rewriteRepoPath(from, to));
            }
            if (message.getAttachments() != null) {
                String to = concat(baseUri, "rest/repo/file",  msg.getRepoPath());
                message.getAttachments().forEach(att -> att.rewriteRepoPath( msg.getRepoPath(), to));
            }
        }

        return message;
    }


    /***************************
     * Publication end-points
     ***************************/


    /**
     * Searches for publications
     */
    @ApiOperation(
            value = "Returns the publications",
            response = PublicationVo.class,
            responseContainer = "List",
            tags = {"publications"}
    )
    @GET
    @Path("/publications")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response searchPublications(

            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language,

            @ApiParam(value = "Timestamp (Unix epoch) for the start date of the publications")
            @QueryParam("from") Long from,

            @ApiParam(value = "Timestamp (Unix epoch) for the end date of the publications")
            @QueryParam("to") Long to,

            @ApiParam(value = "Whether to rewrite all embedded links and paths to be absolute URL's", example = "true")
            @QueryParam("externalize") @DefaultValue("true") boolean externalize,

            @ApiParam(value = "The date format to use for JSON date-time encoding. Either 'UNIX_EPOCH' or 'ISO_8601'", example = "UNIX_EPOCH")
            @QueryParam("dateFormat") @DefaultValue("UNIX_EPOCH") JsonDateFormat dateFormat
    ) {

        // If from and to-dates are unspecified, return the publications currently active
        if (from == null && to == null) {
            from = to = System.currentTimeMillis();
        }

        List<PublicationVo> publications = super.searchPublications(language, from, to).stream()
                .map(p -> toPublicationVo(p, language, externalize))
                .collect(Collectors.toList());

        // Depending on the dateFormat param, either use UNIX epoch or ISO-8601
        StreamingOutput stream = os -> objectMapperForDateFormat(dateFormat).writeValue(os, publications);

        return Response
                .ok(stream, MediaType.APPLICATION_JSON_TYPE)
                .build();
    }


    /**
     * Returns the details for the given publications
     */
    @ApiOperation(
            value = "Returns the publication with the given ID",
            response = PublicationVo.class,
            tags = {"publications"}
    )
    @GET
    @Path("/publication/{publicationId}")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response publicationDetails(

            @ApiParam(value = "The publication ID", example = "5eab7f50-d890-42d9-8f0a-d30e078d3d5a")
            @PathParam("publicationId") String publicationId,

            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language,

            @ApiParam(value = "Whether to rewrite all embedded links and paths to be absolute URL's", example = "true")
            @QueryParam("externalize") @DefaultValue("true") boolean externalize,

            @ApiParam(value = "The date format to use for JSON date-time encoding. Either 'UNIX_EPOCH' or 'ISO_8601'", example = "UNIX_EPOCH")
            @QueryParam("dateFormat") @DefaultValue("UNIX_EPOCH") JsonDateFormat dateFormat
    ) {

        Publication publication = super.getPublication(publicationId);

        if (publication == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("No publication found with ID: " + publicationId)
                    .build();
        } else {

            // Convert publication to value objects and externalize publication links, if requested
            PublicationVo result = toPublicationVo(publication, language, externalize);

            // Depending on the dateFormat param, either use UNIX epoch or ISO-8601
            StreamingOutput stream = os -> objectMapperForDateFormat(dateFormat).writeValue(os, result);

            return Response
                    .ok(stream, MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }


    /**
     * Convert the publication to a value object representation.
     * If requested, rewrite all links to make them external URLs.
     * @param pub the publication to convert to a value object
     * @param externalize whether to rewrite all links to make them external URLs
     * @return the publication value object representation
     **/
    private PublicationVo toPublicationVo(Publication pub, String language, boolean externalize) {

        // Sanity check
        if (pub == null) {
            return null;
        }

        // Convert the publication to a value object
        DataFilter filter = DataFilter.get().lang(language);
        PublicationVo publication = pub.toVo(PublicationVo.class, filter);

        // If "externalize" is set, rewrite all links to make them external
        if (externalize) {
            String baseUri = app.getBaseUri();

            if (publication.getDescs() != null) {
                publication.getDescs().stream()
                        .filter(d -> StringUtils.isNotBlank(d.getLink()))
                        .filter(d -> !d.getLink().matches("^(?i)(https?)://.*$"))
                        .forEach(d -> d.setLink(concat(baseUri, d.getLink())));
            }
        }

        return publication;
    }


    /***************************
     * Area end-points
     ***************************/


    /**
     * Returns the details for the given area
     */
    @ApiOperation(
            value = "Returns the area with the given ID",
            response = AreaVo.class,
            tags = {"areas"}
    )
    @GET
    @Path("/area/{areaId}")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response areaDetails(

            @ApiParam(value = "The area ID", example = "urn:mrn:iho:country:dk")
            @PathParam("areaId") String areaId,

            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language
    ) {

        Area area = super.getArea(areaId);

        if (area == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("No area found with ID: " + areaId)
                    .build();
        } else {

            // Convert area to value objects
            AreaVo result = area.toVo(AreaVo.class, DataFilter.get().fields(DataFilter.PARENT).lang(language));

            return Response
                    .ok(result, MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }


    /**
     * Returns the details for the given area
     */
    @ApiOperation(
            value = "Returns the sub-areas of the area with the given ID",
            response = AreaVo.class,
            responseContainer = "List",
            tags = {"areas"}
    )
    @GET
    @Path("/area/{areaId}/sub-areas")
    @Produces({"application/json;charset=UTF-8"})
    @GZIP
    public Response subAreas(

            @ApiParam(value = "The area ID", example = "urn:mrn:iho:country:dk")
            @PathParam("areaId") String areaId,

            @ApiParam(value = "Two-letter ISO 639-1 language code", example = "en")
            @QueryParam("lang") String language
    ) {

        Area area = super.getArea(areaId);

        if (area == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("No area found with ID: " + areaId)
                    .build();
        } else {

            // NB: Area.getChildren() will return sub-areas in sibling sort order
            List<AreaVo> result = area.getChildren().stream()
                    .filter(Area::isActive)
                    .map(a -> a.toVo(AreaVo.class, DataFilter.get().lang(language)))
                    .collect(Collectors.toList());

            return Response
                    .ok(result, MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }


    /***************************
     * Utility functions
     ***************************/


    /** Returns an ObjectMapper for the given date format **/
    private ObjectMapper objectMapperForDateFormat(JsonDateFormat dateFormat) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                dateFormat == JsonDateFormat.UNIX_EPOCH);
        return mapper;
    }


    /** Concatenates the URI components **/
    private String concat(String... paths) {
        StringBuilder result = new StringBuilder();
        if (paths != null) {
            Arrays.stream(paths)
                    .filter(StringUtils::isNotBlank)
                    .forEach(p -> {
                        if (result.length() > 0 && !result.toString().endsWith("/") && !p.startsWith("/")) {
                            result.append("/");
                        } else if (result.toString().endsWith("/") && p.startsWith("/")) {
                            p = p.substring(1);
                        }
                        result.append(p);
                    });
        }
        return result.toString();
    }

}
