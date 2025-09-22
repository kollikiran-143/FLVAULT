package in.fl.vault;

import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.jdbc.JDBCAppender;
import org.apache.log4j.spi.LoggingEvent;

public class FlVaultJDBCAppender extends JDBCAppender {

	protected String getLogStatement(LoggingEvent event) {

		if (null != event.getThrowableInformation()
				&& event.getThrowableInformation().getThrowable() instanceof SQLException) {
			SQLException myException = new SQLException(
					event.getThrowableInformation().getThrowable().getMessage().replaceAll("'", " "),
					event.getThrowableInformation().getThrowable());
			LoggingEvent logEvent = new LoggingEvent(event.fqnOfCategoryClass,
					LogManager.getLogger(event.getLoggerName()), event.getLevel(),
					event.getThrowableInformation().getThrowable().getCause(), myException);
			String formattedString = getLayout().format(logEvent);
			return formattedString;
		}
		return getLayout().format(event);
	}
}
