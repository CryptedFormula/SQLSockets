package org.cryptedformula.sqs;

import java.util.List;

/**
 * @author Giovanni
 */
public class Response {

	private String description;
	private Players players;
	private Version version;
	private String favicon;
	private int time;

	public String getDescription() {
		return description;
	}

	public String favicon() {
		return favicon;
	}

	public Version getVersion() {
		return version;
	}

	public int time() {
		return time;
	}

	public Players getPlayers() {
		return players;
	}

	public class Players {

		private int max;
		private int online;
		private List<Player> sample;

		public int getMax() {
			return max;
		}

		public int getOnline() {
			return online;
		}

		public List<Player> l() {
			return sample;
		}
	}

	public class Player {

		private String name;
		private String id;

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

	}

	public class Version {

		private String name;
		private String protocol;

		// can also be accessed using Version.name.* / Version.protocol.*
		public String getProtocol() {
			return protocol;
		}

		public String getName() {
			return name;
		}
	}
}