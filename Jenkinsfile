
#!/usr/bin/env groovy
@Library('pipeline@refs/remotes/origin/pr/${env.CHANGE_ID}')
import net.zonarsystems.pipeline.ApplicationPipeline

applicationPipeline = new ApplicationPipeline(
  steps, 
  'busybox', 
  this
)
applicationPipeline.init()
applicationPipeline.pipelineRun()