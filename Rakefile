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

desc "Packages the app for release"
task :package => :build do
  mkdir_p "package"
  cp "Procfile", "package"
  sh "lein uberjar"
  cp "target/asid-0.0.1-SNAPSHOT-standalone.jar", "package/asid.jar"
end

desc "Releases the app"
task :release => :package do
  sh "heroku push package --app asidentity-org"
end

desc "Starts server"
task :start => :build do
  sh "lein ring server"
end

task :default => :start
