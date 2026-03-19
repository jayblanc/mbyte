/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.store.api.resources;

import fr.jayblanc.mbyte.store.api.dto.SearchConversationRequest;
import fr.jayblanc.mbyte.store.search.SearchConversationResult;
import fr.jayblanc.mbyte.store.search.SearchResult;
import fr.jayblanc.mbyte.store.search.SearchService;
import fr.jayblanc.mbyte.store.search.SearchServiceException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("search")
public class SearchResource {

    private static final Logger LOGGER = Logger.getLogger(SearchResource.class.getName());

    @Inject SearchService service;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SearchResult> search(@QueryParam("q") String query) throws SearchServiceException {
        LOGGER.log(Level.INFO, "GET /api/search");
        return service.search(query);
    }

    @POST
    @Path("conversation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchConversationResult converse(SearchConversationRequest request) throws SearchServiceException {
        LOGGER.log(Level.INFO, "POST /api/search/conversation");
        if (request == null) {
            return service.converse(null, null);
        }
        return service.converse(request.getQuery(), request.getConversationId());
    }

    @POST
    @Path("conversation/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Response converseStream(SearchConversationRequest request) throws SearchServiceException {
        LOGGER.log(Level.INFO, "POST /api/search/conversation/stream");
        String query = request != null ? request.getQuery() : null;
        String conversationId = request != null ? request.getConversationId() : null;
        InputStream stream = service.streamConversation(query, conversationId);
        StreamingOutput output = os -> {
            try (stream) {
                stream.transferTo(os);
                os.flush();
            }
        };
        return Response.ok(output).type(MediaType.SERVER_SENT_EVENTS).build();
    }
}
