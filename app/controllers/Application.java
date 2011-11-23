package controllers;

import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.lang.StringUtils;

import play.mvc.Controller;

/**
 * The main application controller.
 * 
 * @author Niklas Ekman (nike@redpill-linpro.com)
 */
public class Application extends Controller {

	/**
	 * The default action, renders the view according to the folder passed. If
	 * no folder is passed the root folder of the repository is rendered.
	 * 
	 * @param folderId
	 *            the folder view to render.
	 */
	public static void index(String folderId) {
		// we do not cache the session, we create it each time
		Session session = createSession();

		Folder folder;
		Folder parent;

		if (StringUtils.isBlank(folderId)) {
			folder = session.getRootFolder();
			parent = null;
		} else {
			folder = (Folder) session.getObject(folderId);
			parent = folder.getFolderParent();
		}

		String[] breadcrumb = createBreadcrumb(folder);

		ItemIterable<CmisObject> children = folder.getChildren();

		Comparator<FileableCmisObject> comparator = createComparator();

		Set<Folder> folders = new TreeSet<Folder>(comparator);

		Set<Document> documents = new TreeSet<Document>(comparator);

		for (CmisObject child : children) {
			if (child instanceof Folder) {
				folders.add((Folder) child);
			} else if (child instanceof Document) {
				documents.add((Document) child);
			}
		}

		render(breadcrumb, parent, documents, folders);
	}

	/**
	 * Action for downloading a document based on a document ID.
	 * 
	 * @param documentId
	 *            the document to download.
	 */
	public static void download(String documentId) {
		// we do not cache the session, we create it each time
		Session session = createSession();

		// get a document instance from the id
		Document document = (Document) session.getObject(documentId);

		// get the mimetype
		String mimetype = document.getContentStreamMimeType();

		// get the filename
		String filename = document.getContentStreamFileName();

		// set the content type
		response.setContentTypeIfNotSet(mimetype);

		// get the data
		ContentStream binaryData = document.getContentStream();

		// render the data back to the browser
		renderBinary(binaryData.getStream(), filename);
	}

	/**
	 * Method for splitting a folder path into an array. The path must be
	 * separated by '/'.
	 * 
	 * @param folder
	 *            to separate
	 * @return an array with the different parts
	 */
	private static String[] createBreadcrumb(Folder folder) {
		String path = folder.getPath();

		return StringUtils.split(path, "/");
	}

	/**
	 * Creates a comparator which sorts on name.
	 * 
	 * @return the comparator
	 */
	private static Comparator<FileableCmisObject> createComparator() {
		return new Comparator<FileableCmisObject>() {

			@Override
			public int compare(FileableCmisObject o1, FileableCmisObject o2) {
				return o1.getName().compareTo(o2.getName());
			}

		};
	}

	/**
	 * Creates a CMIS session based on application configuration settings
	 * 
	 * @return the created session
	 */
	private static Session createSession() {
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();

		Map<String, String> parameters = new HashMap<String, String>();

		// user credentials
		parameters.put(SessionParameter.USER,
				play.Play.configuration.getProperty("cmis.username"));
		parameters.put(SessionParameter.PASSWORD,
				play.Play.configuration.getProperty("cmis.password"));

		// connection settings
		parameters.put(SessionParameter.ATOMPUB_URL,
				play.Play.configuration.getProperty("cmis.url"));
		parameters.put(SessionParameter.BINDING_TYPE,
				BindingType.ATOMPUB.value());

		// session locale
		parameters.put(SessionParameter.LOCALE_ISO3166_COUNTRY,
				play.Play.configuration.getProperty("cmis.country"));
		parameters.put(SessionParameter.LOCALE_ISO639_LANGUAGE,
				play.Play.configuration.getProperty("cmis.language"));

		// put the first repository ID found in the parameter list
		List<Repository> repositories = sessionFactory
				.getRepositories(parameters);
		parameters.put(SessionParameter.REPOSITORY_ID, repositories.get(0)
				.getId());

		// create session
		return sessionFactory.createSession(parameters);
	}

}