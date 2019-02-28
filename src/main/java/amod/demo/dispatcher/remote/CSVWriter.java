package amod.demo.dispatcher.remote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import amod.demo.ext.UserReferenceFrames;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.prep.PopulationTools;
import ch.ethz.idsc.amodeus.prep.Request;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public enum CSVWriter {
	;

	public static void writeCSVFile(Network network) {

		try (PrintWriter writer = new PrintWriter(new File("Berlin.csv"))) {

			StringBuilder sb = new StringBuilder();
			sb.append("id");
			sb.append(',');
			sb.append("Latitude");
			sb.append(',');
			sb.append("Longitude");
			sb.append(',');
			sb.append("ToNde");
			sb.append(',');
			sb.append("Velocity");
			sb.append(',');
			sb.append("Capacity");
			sb.append(',');
			sb.append("Distance");
			sb.append('\n');

			Set<Id<Link>> links = network.getLinks().keySet();

			for (Id<Node> node : network.getNodes().keySet()) {

				Node nodeFrom = network.getNodes().get(node);
				String nodeID = nodeFrom.getId().toString();
				sb.append(nodeID);
				sb.append(',');
				Coord coord = nodeFrom.getCoord();
				Coord coordWGS = UserReferenceFrames.BERLIN.coords_toWGS84().transform(coord);
				double latitude = coordWGS.getY();
				sb.append(latitude);
				sb.append(',');
				double longitude = coordWGS.getX();
				sb.append(longitude);
				sb.append(',');
				Set<Id<Link>> outlinks = nodeFrom.getOutLinks().keySet();
				if (!outlinks.isEmpty()) {
					for (Id<Link> link : outlinks) {
						Link outlink = network.getLinks().get(link);
						Node toNode = outlink.getToNode();
						GlobalAssert.that(nodeFrom == outlink.getFromNode());
						String toNodeStringID = toNode.getId().toString();
						sb.append(toNodeStringID + "+");
					}
					sb.append(',');
				} else {
					sb.append(" ");
					sb.append(',');
				}
				if (!outlinks.isEmpty()) {
					for (Id<Link> link : outlinks) {
						Link outlink = network.getLinks().get(link);
						Node toNode = outlink.getToNode();
						GlobalAssert.that(nodeFrom == outlink.getFromNode());
						double velocity = outlink.getFreespeed();
						sb.append(velocity);
						sb.append("+");
					}
					sb.append(',');
				} else {
					sb.append(" ");
					sb.append(',');
				}
				if (!outlinks.isEmpty()) {
					for (Id<Link> link : outlinks) {
						Link outlink = network.getLinks().get(link);
						Node toNode = outlink.getToNode();
						GlobalAssert.that(nodeFrom == outlink.getFromNode());
						double capacity = outlink.getCapacity();
						sb.append(capacity);
						sb.append("+");
					}
					sb.append(',');
				} else {
					sb.append(" ");
					sb.append(',');
				}
				if (!outlinks.isEmpty()) {
					for (Id<Link> link : outlinks) {
						Link outlink = network.getLinks().get(link);
						Node toNode = outlink.getToNode();
						GlobalAssert.that(nodeFrom == outlink.getFromNode());
						double distance = outlink.getLength();
						sb.append(distance);
						sb.append("+");
					}
					sb.append(',');
				} else {
					sb.append(" ");
					sb.append(',');
				}

				sb.append('\n');
			}

			writer.write(sb.toString());

			System.out.println("done!");

		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}

	}

	public static void writeCSVFileRequests(Network network, Config config) {

		try (PrintWriter writer = new PrintWriter(new File("BerlinRequests.csv"))) {
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Population population = scenario.getPopulation();
			int endTime = (int) config.qsim().getEndTime();
			Set<Request> avRequests = PopulationTools.getAVRequests(population, network, endTime);

			StringBuilder sb = new StringBuilder();
			sb.append("id origin");
			sb.append(',');
			sb.append("id destination");
			sb.append(',');
			sb.append("start time");
			sb.append('\n');

			Set<Id<Link>> links = network.getLinks().keySet();

			for (Request req : avRequests) {

				Id<Node> nodeFrom = req.startLink().getFromNode().getId();
				String nodeID = nodeFrom.toString();
				sb.append(nodeID);
				sb.append(',');
				Id<Node> nodeTo = req.endLink().getToNode().getId();
				String nodeIDTo = nodeTo.toString();
				sb.append(nodeIDTo);
				sb.append(',');
				double starttime = req.startTime();
				sb.append(starttime);
				sb.append('\n');
			}

			writer.write(sb.toString());

			System.out.println("done!");

		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}

	}

}
