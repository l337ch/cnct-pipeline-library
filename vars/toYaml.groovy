#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return toYaml(config.obj)
}

@NonCPS
def toYaml(serializedObject) {
  DumperOptions options = new DumperOptions();
  options.setDefaultFlowStyle(FlowStyle.BLOCK);
  options.setPrettyFlow(true);

  Yaml yaml = new Yaml(options);
  return yaml.dump(serializedObject)
}