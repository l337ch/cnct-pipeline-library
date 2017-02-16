CHARTS := $(shell find . -path '*/Chart.yaml' | tr '\n' ' ' | sed -E 's:\./|/Chart\.yaml::g')
DEP_CHARTS := $(shell find . -path '*/requirements.yaml' | tr '\n' ' ' |  sed -E 's:\./|/requirements\.yaml::g')
CHART_REPO = "change-me"
REPO_BUCKET = "change-me"

.PHONY: clean all package makepath copy index sync acl dependency-update
all: package makepath copy index sync clean

dependency-update:
	helm init -c
	helm repo add pipeline-charts $(CHART_REPO)
	$(foreach chart,$(DEP_CHARTS),(helm dependency update --debug $(chart); echo $?) && ) :

lint:
	$(foreach chart,$(CHARTS),(helm lint $(chart)) &&) :

package: dependency-update ; $(foreach chart,$(CHARTS),(helm package $(chart) --save=false) &&) :

makepath:
	@mkdir -p .pipeline-charts

copy:
	@mv *.tgz .pipeline-charts/

index:
	@gsutil -h Cache-Control:private -m cp $(REPO_BUCKET)/index.yaml .pipeline-charts/index.yaml
	@helm repo index .pipeline-charts --url $(CHART_REPO) --merge .pipeline-charts/index.yaml

sync:
	@gsutil -h Cache-Control:private -m cp -r .pipeline-charts/* $(REPO_BUCKET)
	
clean:
	@rm -rf .pipeline-charts