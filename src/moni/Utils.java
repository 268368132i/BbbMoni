package moni;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import moni.List.RecordingClass;

class Recording {
	Document doc;
	File file;
	String state;
	
	Recording(File file, String state) throws SAXException, IOException, ParserConfigurationException{
		this(file);
		this.state = state;
	}
	Recording(File file) throws SAXException, IOException, ParserConfigurationException{
		this.file = file;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc = builder.parse(file);
	}
	
	public String getName() {
		//System.out.println("Names: " + doc.getElementsByTagName("bbb-recording-name").getLength());
		if (doc.getElementsByTagName("bbb-recording-name").getLength()<1) return "N/A";
		return doc.getElementsByTagName("bbb-recording-name").item(0).getTextContent();
	}
	
	public void setState(String state) {
		this.state = state;
	}
	public String getState() {
		if (this.state != null) {
			return this.state;
		}
		if (doc.getElementsByTagName("state").getLength()<1) return "";
		return doc.getElementsByTagName("state").item(0).getTextContent();
	}
	public String getLink() {
		if (doc.getElementsByTagName("link").getLength()<1) return "";
		return doc.getElementsByTagName("link").item(0).getTextContent();
	}
	public String getStartTime(SimpleDateFormat sdf) {
		if (doc.getElementsByTagName("start_time").getLength()<1) return sdf.format(new Date(0));
		String milliTime = doc.getElementsByTagName("start_time").item(0).getTextContent();
		Date date = new Date(Long.parseLong(milliTime));
		return sdf.format(date);
	}
	public String getStartTime() {
		return this.getStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"));
	}
	public String getId() {
		if(doc.getElementsByTagName("id").getLength()<1) return "";
		return doc.getElementsByTagName("id").item(0).getTextContent();
	}
	public Long getSize(){
		return Utils.size(file.toPath().getParent());
	}
}

public class Utils {
	/**
	 * Attempts to calculate the size of a file or directory.
	 * 
	 * <p>
	 * Since the operation is non-atomic, the returned value may be inaccurate.
	 * However, this method is quick and does its best.
	 */
	public static long size(Path path) {

	    final AtomicLong size = new AtomicLong(0);

	    try {
	        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

	                size.addAndGet(attrs.size());
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult visitFileFailed(Path file, IOException exc) {

	                System.out.println("skipped: " + file + " (" + exc + ")");
	                // Skip folders that can't be traversed
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

	                if (exc != null)
	                    System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
	                // Ignore errors traversing a folder
	                return FileVisitResult.CONTINUE;
	            }
	        });
	    } catch (IOException e) {
	        throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
	    }

	    return size.get();
	}
	public static String convertToHumanSize(Long size) {
		BigDecimal convSize;
		String units[] = {"","K","M","G","T"};
		
		for (int i=0; i<units.length;i++) {
			convSize = new BigDecimal (size / java.lang.Math.pow(1024, i));
			MathContext m = new MathContext(4);
			convSize = convSize.round(m);
			if (convSize.longValue()<1000) {				
				return convSize.floatValue() + units[i] + "B";
			}
		}
		return size + "B";
	}
    public static ArrayList<File> listDirs(String path, RecordingClass type, String postfix){
    	if (type == null) {
    		System.out.println("Recording class/type is null. Aborting.");
    		return null;
    	}
    	if (postfix == null) postfix = "presentation";
    	String recPath;
    	switch(type) {
    	case PUBLISHED : recPath = "published";
    		break;
    	case UNPUBLISHED : recPath ="unpublished";
    		break;
    	case DELETED : recPath = "deleted";
    		break;
    	default : 
    		System.out.println("Not found REcording Path");
    		return null;
    	}
    	
    	File dir = new File(path + "/" + recPath + "/" + postfix +"/");
    	//System.out.println(dir.getAbsolutePath());
    	ArrayList<File> recordings = new ArrayList<File>();
    	int counter=0;
    	for (File f : dir.listFiles()) {
    		if (f.isDirectory()) recordings.add(f);
    		counter++;
    	}
    	//System.out.println("Counter=" + counter);
    	return recordings;

    }
    public static String getRecPath(RecordingClass rc) {
    	switch(rc) {
    	case PUBLISHED : return "published";
    	case UNPUBLISHED : return "unpublished";
    	case DELETED : return "deleted";
    	default : 
    		System.out.println("Not found REcording Path");
    		return null;
    	}
    }
    
   
	public static Document getXMLDoc(File file) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		//System.out.println("Encoding:" + doc.getXmlEncoding() + " of " + file.getAbsolutePath());
		return doc;
	}
	
	public static String getRecordingName(Document doc) {
		//System.out.println(doc.getElementsByTagName("bbb-recording-name").item(0).getTextContent());
		return doc.getElementsByTagName("bbb-recording-name").item(0).getTextContent();
	}
	public static String getRecordingStartTime(Document doc, SimpleDateFormat sdf) {
		String milliTime = doc.getElementsByTagName("start_time").item(0).getTextContent();
		Date date = new Date(Long.parseLong(milliTime));
		return sdf.format(date);
	
	}
	public static String getRecordingStartTime(Document doc) {
		return getRecordingStartTime(doc, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"));
	}
	
	public static String getRecordingId(Document doc) {
		return doc.getElementsByTagName("id").item(0).getTextContent();
	}
	
	public static ArrayList<Recording> getRecordings(ArrayList<File> dirs, RecordingClass rc) throws ParserConfigurationException, SAXException, IOException{
		ArrayList<Recording> recs = new ArrayList<Recording>();
		if (dirs == null) return recs;
		for (File dir : dirs) {
			File metadata = new File(dir.getAbsolutePath() + "/metadata.xml");
			//Document doc = getXMLDoc(metadata);
			recs.add(new Recording(metadata, getRecPath(rc)));
		}
		//System.out.println("Dirs:" + dirs.size() + "; Recs:" + recs.size());
		return recs;
	}
	public static String printRecording(Document rec) {
		String tag = "<div class=\"pure-g\" id=\"r_" + getRecordingId(rec) + "\">";
		tag+="<div class=\"pure-u-1-5 \">" + getRecordingName(rec) + "</div>";
		tag+="<div class=\"pure-u-1-12\">" + getRecordingStartTime(rec) + "</div>";
		tag+="</div>";
		return tag;
	}
	public static String printRecording(Recording rec) {
		String tag = "<div class=\"pure-g hover\" id=\"r_" + rec.getId() + "\">";
		if (rec.getState().equals("published")) tag+="<div class=\"pure-u-1-5 xspad\"><a target=\"_blank\" href=\""+rec.getLink()+"\">" + rec.getName() + "</a></div>";
		else tag+="<div class=\"pure-u-1-5 xspad\">" + rec.getName() + "</div>";
		tag+="<div class=\"pure-u-1-12 xspad\">" + rec.getStartTime() + "</div>";
		tag+="<div class=\"pure-u-1-24 xspad\">" + Utils.convertToHumanSize(rec.getSize()) + "</div>";
		// Actions
		tag+="<div class =\"pure-u-1-12\">";
		if (rec.getState().equals("published")) {
			tag+="<div class=\"pure-u-1-24 xspad\"><a class=\"pure-button pure-button-primary\" onclick='sendCmd(this.parentNode.parentNode,\"unpublish\",\"" + rec.getId() + "\")'>Unpublish</a></div>";
		} else if (rec.getState().equals("unpublished")) {
			tag+="<div class=\"pure-u-1-24 xspad\"><a class=\"pure-button pure-button-primary\" onclick='sendCmd(this.parentNode.parentNode,\"publish\",\"" + rec.getId() + "\")'>Publish</a></div>";
		} else if (rec.getState().equals("deleted")) tag+="<div class=\"pure-u-1-24 xspad\"><a class=\"pure-button button-error\" onclick='sendCmd(this.parentNode.parentNode,\"erase\",\"" + rec.getId() + "\")'>Erase</a></div>";
		tag+="<div class=\"pure-u-1-24 xspad\"><a class=\"pure-button pure-button-primary\" href='get?recording=" + rec.getId() + "&path="+rec.getState()+"'>Download</a></div>";
		tag+="</div>";
		return tag;
	}
	
	public static boolean deleteNIO2(Path path) throws IOException {
			    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			        @Override
			        public FileVisitResult postVisitDirectory(
			          Path dir, IOException exc) throws IOException {
			        	//System.out.println("Deleting " + dir.toString());
			            Files.delete(dir);
			            return FileVisitResult.CONTINUE;
			        }
			        
			        @Override
			        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			        	//System.out.println("Deleting " + file.toString());
			            Files.delete(file);
			            return FileVisitResult.CONTINUE;
			        }
			    });
			    if(Files.exists(path)) {
			    	System.out.println("Files still exist");
			    	return false;
			    }
			    return true;
			}
	
	public static void  zipFile(File f2z,String fileName,/* OutputStream out,*/ ZipOutputStream zip) throws IOException {
		//File f2z = new File(path);
		//ZipOutputStream zip = new ZipOutputStream(out);
		System.out.println("Compressing " + fileName);
		if (f2z.isHidden()) return;
		if (f2z.isDirectory()) {
			if (fileName.endsWith("/")) {
				zip.putNextEntry(new ZipEntry(fileName));
				zip.closeEntry();
			} else {
				zip.putNextEntry(new ZipEntry(fileName + "/"));
				zip.closeEntry();				
			}
			File[] children = f2z.listFiles();
			for(File child : children) {
				zipFile(child, fileName +"/"+ child.getName(),zip);
			}
			return;
		}
		FileInputStream in = new FileInputStream(f2z);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zip.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while((length=in.read(bytes))>=0) {
			System.out.println("Writing " + length + " bytes to archive.");
			zip.write(bytes,0,length);
		}
		in.close();
	}
	public static void pack(String sourceDirPath, OutputStream out) throws IOException {
	    //Path p = Files.createFile(Paths.get(zipFilePath));
	    try (/*ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))*/ ZipOutputStream zs = new ZipOutputStream(out)) {
	        Path pp = Paths.get(sourceDirPath);
	        Files.walk(pp)
	          .filter(path -> !Files.isDirectory(path))
	          .forEach(path -> {
	              ZipEntry zipEntry = new ZipEntry(pp.getParent().relativize(pp).toString() +"/"+ pp.relativize(path).toString());
	              try {
	                  zs.putNextEntry(zipEntry);
	                  Files.copy(path, zs);
	                  zs.closeEntry();
	            } catch (IOException e) {
	                System.err.println(e);
	            }
	          });
	    }
	}
    public static String getMedDir(String in) {
    	switch(in) {
    	case "published": return "published";
    	case "pub": return "published";
    	case "unpublished": return "unpublished";
    	case "unp": return "unpublished";
    	case "deleted": return "deleted";
    	case "del": return "deleted";
    	default: return null;
    	}
    }
}