'use strict';

var path      = require('path');

var gulp      = require('gulp');
var less      = require('gulp-less');
var rename    = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var prefix    = require('gulp-autoprefixer');

var SRC_DIR = path.join(__dirname, 'src', 'docs', 'style');
var OUT_DIR  = path.join(__dirname, 'build', 'docs', 'style');

gulp.task('styles', function() {
  gulp.src(path.join(SRC_DIR, 'main.less'))
    .pipe(less( ))

    // prefix
    .pipe(prefix( { cascade: false, browsers: ['> 0%', 'last 2 versions', 'Firefox ESR', 'Opera 12.1'], } ) )
    .pipe(gulp.dest(OUT_DIR))

    // minify
    .pipe(rename( { suffix: ".min" } ))
    .pipe(minifyCss())
    .pipe(gulp.dest( OUT_DIR ))
});

gulp.task('default', ['styles']);