namespace :build do

  task :compass do
    sh "compass compile ."
  end

end

desc "Builds all static files for the web app"
task :build => ['build:compass']
