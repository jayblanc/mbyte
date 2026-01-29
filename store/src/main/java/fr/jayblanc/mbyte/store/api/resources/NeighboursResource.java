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

import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.topology.TopologyConfig;
import fr.jayblanc.mbyte.store.topology.TopologyException;
import fr.jayblanc.mbyte.store.topology.TopologyService;
import fr.jayblanc.mbyte.store.topology.entity.Neighbour;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("network")
public class NeighboursResource {

    private static final Logger LOGGER = Logger.getLogger(NeighboursResource.class.getName());

    @Inject TopologyConfig config;
    @Inject TopologyService neighbourhood;
    @Inject AuthenticationService auth;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Neighbour> getNeighbours() throws TopologyException {
        LOGGER.log(Level.INFO, "GET /api/network");
        return neighbourhood.list();
    }

}
