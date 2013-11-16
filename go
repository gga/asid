#!/usr/bin/env bash

git pull --rebase && git push origin master && lein midje && bundle exec rake release

