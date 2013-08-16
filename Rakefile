def haml(output, tmpl)
  (File.join(output, File.basename(tmpl, '.haml')) + '.html').tap do |html_file|
    file html_file => tmpl do
      sh "haml #{tmpl} #{html_file}"
    end
  end
end

namespace :build do

  task :haml

  Dir['src/**/*.haml'].each { |tmpl| task :haml => haml(File.dirname(tmpl), tmpl) }
  Dir['static/pages/*.haml'].each { |tmpl| task :haml => haml('resources/public', tmpl) }

  task :compass do
    sh "compass compile ."
  end

end

desc "Builds all static files for the web app"
task :build => ['build:haml', 'build:compass']
