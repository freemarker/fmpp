'use strict';

var path      = require('path');

var gulp      = require('gulp');
var less      = require('gulp-less');
var rename    = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var prefix    = require('gulp-autoprefixer');

var BASE_DIR = path.join(__dirname, 'src', 'docs');
var OUT_DIR  = path.join(BASE_DIR, 'style');

gulp.task('styles', function() {
  gulp.src(path.join(BASE_DIR, 'less', 'styles.less'))
    .pipe( less( ) )

    // rename and prefix
    .pipe( rename( { basename: "main" } ) )
    .pipe( prefix( { cascade: false } ) )
    .pipe( gulp.dest( OUT_DIR ) )

    // minify
    .pipe( rename( { suffix: ".min" } ) )
    .pipe( minifyCss() )
    .pipe( gulp.dest( OUT_DIR ) )
});

gulp.task('default', ['styles']);