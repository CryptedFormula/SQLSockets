package org.cryptedformula.sqs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * @author Giovanni
 */
public class TimeUtil {

	public String formatDateDiff(LocalDateTime fromDate, LocalDateTime toDate) {
		if (toDate.equals(fromDate)) {
			return "now";
		}
		boolean future = false;
		if (toDate.isAfter(fromDate)) {
			future = true;
		}

		StringBuilder sb = new StringBuilder(16);
		ChronoUnit[] types = { ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS,
				ChronoUnit.MINUTES, ChronoUnit.SECONDS };
		String[] names = { "year", "years", "month", "months", "day", "days", "hour", "hours", "minute", "minutes",
				"second", "seconds" };
		for (int i = 0; i < 6; i++) {
			int diff = dateDiff(types[i], fromDate, toDate, future);
			if (diff > 0) {
				sb.append(' ').append(diff).append(' ');
				if (diff > 1) {
					sb.append(names[(i << 1) + 1]);
				} else {
					sb.append(names[i << 1]);
				}
			}
		}
		if (sb.length() == 0) {
			return "now";
		}
		return sb.toString().trim();
	}

	public String formatDateDiff(long date) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime diff = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneOffset.UTC);
		return formatDateDiff(now, diff);
	}

	private int dateDiff(ChronoUnit type, LocalDateTime fromDate, LocalDateTime toDate, boolean future) {
		int diff = 0;
		while ((future && !fromDate.isAfter(toDate)) || (!future && !fromDate.isBefore(toDate))) {
			fromDate.plus(future ? 1 : -1, type);
			diff++;
		}
		diff--;
		fromDate.minus(future ? 1 : -1, type);
		return diff;
	}
}