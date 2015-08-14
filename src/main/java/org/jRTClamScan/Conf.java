package org.jRTClamScan;

import java.util.ArrayList;

public class Conf {
	private String mailserver=null;
	private String mailfrom=null;
	private String mailto=null;
	private ArrayList<FolderMonitor> foldermonitors=null;
	
	public String getMailserver() {
		return mailserver;
	}
	
	public void setMailserver(String mailserver) {
		this.mailserver=mailserver;
	}
	
	public String getMailfrom() {
		return mailfrom;
	}
	
	public void setMailfrom(String mailfrom) {
		this.mailfrom=mailfrom;
	}
	
	public String getMailto() {
		return mailto;
	}
	
	public void setMailto(String mailto) {
		this.mailto=mailto;
	}
	
	public ArrayList<FolderMonitor> getFoldermonitors() {
		return foldermonitors;
	}
	
	public void setFoldermonitors(ArrayList<FolderMonitor> foldermonitors) {
		this.foldermonitors=foldermonitors;
	}
	
	@Override
	public String toString() {
		String ret="Conf [mailserver="+mailserver+". mailfrom="+mailfrom+", mailto="+mailto;
		
		for(int i=0;i<foldermonitors.size();i++) {
			if(i==0)ret+=", ";
			FolderMonitor fm=foldermonitors.get(i);
			ret+="FolderMonitor [folder="+fm.getFolder()+", corepoolsize="+fm.getCorepoolsize()+", maxpoolsize="+fm.getMaxpoolsize()+", keepalive="+fm.getKeepalive()+", exclude="+fm.getExclude()+", maxfilesize="+fm.getMaxfilesize()+"], ";
		}
		
		ret+="]";
		
		return ret;
	}
}
