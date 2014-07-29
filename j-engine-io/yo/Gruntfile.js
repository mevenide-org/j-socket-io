module.exports = function(grunt) {
    var path = require('path');

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
                                    'var common = require(\'../common\');\n' +
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
        }
    });

    grunt.registerTask('test', ['copy:test', 'jshint', 'mochaTest']);

    grunt.registerTask('default', ['clean', 'jshint', 'copy:build' ]);
};
