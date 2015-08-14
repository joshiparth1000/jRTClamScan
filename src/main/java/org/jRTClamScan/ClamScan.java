package org.jRTClamScan;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClamScan implements Runnable {
	private File scanfile=null;
	private static final Logger logger=LogManager.getLogger(ClamScan.class);
	
	public ClamScan(File scanfile) {
		this.scanfile=scanfile;
	}

	public void run() {
		Runtime rt=Runtime.getRuntime();
		String []commands={"clamscan","--stdout","-i",scanfile.getAbsolutePath()};
		
		try {
			logger.info("Starting clamscan on file: "+scanfile.getAbsolutePath());
			Process proc=rt.exec(commands);
			
			int retcode=proc.waitFor();
			logger.info("Finished clamscan on file: "+scanfile.getAbsolutePath());
			if(retcode!=0) {
				String subject=null;
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String stdout="";
				String s=null;
				
				while((s=stdInput.readLine())!=null)
					stdout+=s+System.getProperty("line.separator");
				
				if(retcode==1) {
					logger.error("Virus infection detected on file:"+scanfile.getAbsolutePath());
					logger.error(stdout);
					subject="Virus infection detected";
				}
				else {
					logger.warn("Error occured during virus scan on file:"+scanfile.getAbsolutePath());
					logger.warn(stdout);
					subject="Error occured during virus scan";
				}
				
				Properties properties=System.getProperties();
				properties.setProperty("mail.smtp.host", App.conf.getMailserver());
				
				Session session=Session.getDefaultInstance(properties);
				
				MimeMessage message=new MimeMessage(session);
				message.setFrom(new InternetAddress(App.conf.getMailfrom()));
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(App.conf.getMailto()));
				message.setSubject(subject);
				message.setText(stdout);
				Transport.send(message);
				
				logger.info("Notification sent to: "+App.conf.getMailto());
			}
		} catch (IOException | InterruptedException e) {
			logger.error("Unable to run clamscan: "+e.getMessage());
		} catch (MessagingException e) {
			logger.error("Unable to send notification: "+e.getMessage());
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

}
