

import groovy.json.*
import org.yaml.snakeyaml.Yaml

class GithubHelpers implements Serializable {

	def githubURLBase = 'https://api.github.com/'
	def orgName = 'samsung-cnct'
  	def githubURL = null
  	def username = null;
  	def autString = null;
  	
	GithubHelpers(repoName, orgName, username) {
	    this.githubURL = "${this.githubURLBase}${orgName}/${repoName}"
	    def authToken = getCredentialsTokenForUser(username)
	    this.authString = "${this.username}:${authToken}".getBytes().encodeBase64().toString();
	    this.username = username
	 }

	@NonCPS
	def getCredentialsTokenForUser(username) {
	  def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
	        com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,
	        Jenkins.instance
	    )
	    def c = creds.findResult { it.username == username ? it : null }
	    if ( c ) {
	        println "found credential ${c.id} for username ${c.username} with password ${c.password}"
	        return c.password
			
	    }
	}
	
	@NonCPS
	def getFoldersForPath(path) {
	    
	    def url = "${githubURL}/${path}"
	    URLConnection githubURL = new URL(url).openConnection();
	    githubURL.setRequestProperty("Authorization", "Basic ${this.authString}");
	    def contents = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(githubURL.getInputStream())));
	    def dirs = [];
	    for ( e in contents ) {
	       if (e.type == 'dir' ) {
	           dirs.add(e.name)
	       }
	    }
	    return dirs;
	}
	
	def loadYAMLFromGithub(filePath) {
	    def url = "${githubURL}/${filePath}"
	    URLConnection conn = new URL(url).openConnection();
	    conn.setRequestProperty("Authorization", "Basic ${this.authString}");
	    def contents = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(conn.getInputStream())));
	    Yaml yml = new Yaml();
	    Map values = (Map)yml.load(new URL(contents.download_url).getText());
	    return values;
	
	}
	
	/**
	 * returns a list of images for a specific chart in this project
	 */
	def getImagesForChart(chartName) {
		
		def url = "${githubURL}/charts/${chartName}"
	    def valuesYAML = loadYAMLFromGithub(url);
	    for (image in valuesYAML.get("images")) {
	        String key = image.getKey();
	        String value = image.getValue();
	        echo "found image: ${key} with value: ${value}"
	    }
	    return valuesYAML.get("images");
	}
	
}