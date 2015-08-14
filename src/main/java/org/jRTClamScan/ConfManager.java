package org.jRTClamScan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

public class ConfManager {
	private Conf conf=new Conf();
	@SuppressWarnings("unused")
	private Marshaller marshaller=null;
	private Unmarshaller unmarshaller=null;
	private static final Logger logger=LogManager.getLogger(ConfManager.class);
	
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller=marshaller;
	}
	
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller=unmarshaller;
	}
	
	public Conf getConf() {
		return this.conf;
	}
	
	public void loadConf() {
		logger.info("Loading configuration file: "+App.CONF_FILE);
		
		FileInputStream is=null;
		
		try {
			is=new FileInputStream(App.CONF_FILE);
			conf=(Conf)unmarshaller.unmarshal(new StreamSource(is));
		} catch (FileNotFoundException e) {
			logger.fatal("Configuration file not found: "+e.getMessage());
			logger.fatal(ExceptionUtils.getStackTrace(e));
			System.exit(1);
		} catch (XmlMappingException e) {
			logger.fatal("XML Mapping error: "+e.getMessage());
			logger.fatal(ExceptionUtils.getStackTrace(e));
			System.exit(1);
		} catch (IOException e) {
			logger.fatal("General IO error: "+e.getMessage());
			logger.fatal(ExceptionUtils.getStackTrace(e));
			System.exit(1);
		} finally {
			if(is!=null)
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Error closing input stream: "+e.getMessage());
					logger.error(ExceptionUtils.getStackTrace(e));
				}
		}
	}
}
