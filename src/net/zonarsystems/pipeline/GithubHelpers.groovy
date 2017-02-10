import groovy.json.*
import org.yaml.snakeyaml.Yaml

class GithubHelpers implements Serializable {

	def githubURLBase = 'https://api.github.com/repos/samsung-cnct/'
  	def githubURL = null
  	
	GithubHelpers(repoName, username) {
	    this.githubURL = "this.githubURLBase/${repoName}"
	    this.authToken = getCredentialsTokenForUser(username)
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
	def getFoldersForRepo(url, username, token) {
	    
	    def authString = "${username}:${token}".getBytes().encodeBase64().toString();
	    URLConnection githubURL = new URL(url).openConnection();
	    githubURL.setRequestProperty("Authorization", "Basic ${authString}");
	    def contents = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(githubURL.getInputStream())));
	    def dirs = [];
	    for ( e in contents ) {
	       if (e.type == 'dir' ) {
	           dirs.add(e.name)
	       }
	    }
	    return dirs;
	}
	
	def loadYAMLFromGithub(url, username, token) {
	    
	    def authString = "${username}:${token}".getBytes().encodeBase64().toString();
	    URLConnection githubURL = new URL(url).openConnection();
	    githubURL.setRequestProperty("Authorization", "Basic ${authString}");
	    def contents = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(githubURL.getInputStream())));
	    Yaml yml = new Yaml();
	    echo "${contents}"
	    Map values = (Map)yml.load(new URL(contents.download_url).getText());
	    return values;
	
	}
	
	def getImagesForChart(valuesURL, username, token) {
	    def valuesYAML = loadYAMLFromGithub(valuesURL, username, token);
	    for (image in valuesYAML.get("images")) {
	        String key = image.getKey();
	        String value = image.getValue();
	        echo "found image: ${key} with value: ${value}"
	    }
	    return valuesYAML.get("images");
	}
	
	
	}
}