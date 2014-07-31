'use strict';

module.exports = function(grunt) {
    var _ = require('lodash'),
        child_process = require('child_process'),
        path = require('path');

    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        clean: {
            build: {
                src: ['build']
            },
            test: {
                src: ['test/generated']
            }
        },
        copy: {
            test: {
                files: [
                    {
                        expand: true,     // Enable dynamic expansion.
                        cwd: 'node_modules/engine.io/test',      // Src matches are relative to this path.
                        src: ['server.js'], // Actual pattern(s) to match.
                        dest: 'test/generated',   // Destination path prefix.
                        rename: function(dest, src) {
                            return path.join(dest, 'java-' + src);
                        }
                    }
                ],
                options: {
                    process: function(content, srcPath) {
                        if (srcPath === 'node_modules/engine.io/test/server.js') {
                            // alter the test to run against our java server
                            content = content
                                .replace(/^var eio = require\('\.\.'\);$/m, 'var eio = require(\'../java-engine.io\');')
                                .replace(/^var listen = require\('\.\/common'\)\.listen;$/m,
                                    'var common = require(\'../java-common\');\n' +
                                    'var listen = common.listen;\n' +
                                    'common.proxyEngineIoClientSocket(eioc);');
                        }
                        return content;
                    }
                }
            }
        },
        jshint: {
            files: ['Gruntfile.js', 'src/**/*.js', 'test/**/*.js', '!test/generated/**/*.js' ],
            options: {
                // options here to override JSHint defaults
                globals: {
                    jQuery: true,
                    console: true,
                    module: true,
                    document: true
                }
            }
        },
        mochaTest: {
            test: {
                options: {
                    reporter: 'spec'
                },
                src: ['test/**/*.js']
            }
        },
        watch: {
            files: ['<%= jshint.files %>'],
            tasks: ['copy:build', 'jshint']
        },
        mvn: {
            classpath: {
                src: ['../pom.xml'],
                dest: 'classpath.list'
            }
        }
    });

    /**
     * We do this in grunt because it's a bit slow to do every-time we run the tests.
     */
    grunt.registerMultiTask('mvn', 'Generate classpath from pom for node-java', function() {
        var that = this,
            done = this.async(),
            counter = this.files.length;
        _.forEach(this.files, function(filePair) {
            checkFilePair(filePair);
            getClasspath(filePair.src, function(classpath) {
                var classpathArray = _.filter(classpath.trim().split(path.delimiter), function(file) {
                    return file && file.trim();
                });
                grunt.file.write(filePair.dest, classpathArray.join('\n'), {encoding: 'utf8'});
                done();
            });
        });

        function checkFilePair(filePair) {
            if (filePair.src.length === 0) {
                throw grunt.util.error('Could not find src file for ' + that.target);
            }
            if (filePair.src.length > 1) {
                throw grunt.util.error(that.target + ' has more than one src file: ' + filePair.src);
            }
        }

        /**
         * Get classpath from pom.
         *
         * @param pom
         * @param cb
         */
        function getClasspath(pom, cb) {
            var cmd = 'mvn -f ' + pom + ' dependency:build-classpath -DincludeScope=test';
            child_process.exec(cmd, function(err, stdout, stderr) {
                if (err) {
                    throw grunt.util.error('Command "' + cmd + '" failed with exit code ' + err.code + ':\n' + stderr);
                }

                var next = false,
                    classpath = _.find(stdout.split('\n'), function(line) {
                        if (next) {
                            return true;
                        } else if (/^\[INFO\] Dependencies classpath:\s*$/.test(line)) {
                            // search for the classpath line
                            next = true;
                        }
                    });

                cb(classpath && classpath.trim() || '');
            });
        }
    });

    grunt.registerTask('test', ['copy:test', 'jshint', 'mochaTest']);

    grunt.registerTask('default', ['clean', 'jshint', 'copy:build' ]);
};
