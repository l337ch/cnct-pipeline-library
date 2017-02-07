#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return unpackDependencyVars(config.upstreamEnv)
}

@NonCPS
def unpackDependencyVars (upstreamEnv) {
  return upstreamEnv.inject([]) { list, entry ->
    def result = []
    if (entry.key.startsWith('HELMPARAM_')) {
      result << "${entry.key.substring(10).replaceAll('_','.')}=${entry.value.toString()}"
    } 
    list += result
  }.join(',')
}