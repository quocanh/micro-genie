package io.microgenie.application.util;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;

/***
 * Safely close resources
 * @author shawn
 */
public class CloseableUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloseableUtil.class); 
	
	/** The Guava version throws an IOException even if swallow exception is set to true, based on guava version **/
	public static void closeQuietly(Closeable closeable){
		try{
			Closeables.close(closeable, true);
		}catch(Exception ex){
			LOGGER.debug(ex.getMessage(),ex);
		}
	}
}
