require 'ggake'

namespace :build do

  task :haml

  Dir['static/pages/**/*.haml'].each do |tmpl|
    task :haml => haml_template(tmpl, output: tmpl.pathmap("%{static,resources;pages,public}d"))
  end

  task :coffee => Dir['static/scripts/*.coffee'].collect { |file| coffee(file, output: 'resources/public/scripts') }

  task :libs do
    cp Dir["static/libs/*.js"], "resources/public/scripts"
  end

  task :compass do
    sh "compass compile ."
  end

end

desc "Builds all static files for the web app"
task :build => ['build:haml', 'build:compass', 'build:coffee', 'build:libs']

desc "Starts server"
task :start => :build do
  sh "lein ring server"
end

task :default => :start
