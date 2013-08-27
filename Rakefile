require 'ggake'

namespace :build do

  task :haml

  Dir['src/**/*.haml'].each { |tmpl| task :haml => haml_template(tmpl, output: File.dirname(tmpl)) }
  Dir['static/pages/*.haml'].each { |tmpl| task :haml => haml_template(tmpl, output: 'resources/public') }

  task :coffee => Dir['static/scripts/*.coffee'].collect { |file| coffee(file, output: 'resources/public/scripts') }

  task :compass do
    sh "compass compile ."
  end

end

desc "Builds all static files for the web app"
task :build => ['build:haml', 'build:compass', 'build:coffee']

desc "Starts server"
task :start => :build do
  sh "lein ring server"
end

task :default => :start
