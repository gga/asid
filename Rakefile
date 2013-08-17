require 'ggake'

namespace :build do

  task :haml

  Dir['src/**/*.haml'].each { |tmpl| task :haml => haml_template(tmpl, output: File.dirname(tmpl)) }
  Dir['static/pages/*.haml'].each { |tmpl| task :haml => haml_template(tmpl, output: 'resources/public') }

  task :compass do
    sh "compass compile ."
  end

end

desc "Builds all static files for the web app"
task :build => ['build:haml', 'build:compass']
