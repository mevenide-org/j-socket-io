module.exports = function(grunt) {
    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        clean: {
            build: {
                src: ['build']
            }
        },
        copy: {
            build: {
                files: [
                    {
                        expand: true,     // Enable dynamic expansion.
                        cwd: 'src',      // Src matches are relative to this path.
                        src: ['index.html'], // Actual pattern(s) to match.
                        dest: 'build/static'   // Destination path prefix.
                    },
                    {
                        expand: true,     // Enable dynamic expansion.
                        cwd: '/',      // Src matches are relative to this path.
                        src: ['src/**/*.js'], // Actual pattern(s) to match.
                        dest: 'build/static/js'   // Destination path prefix.
                    },
                    {
                        expand: true,     // Enable dynamic expansion.
                        cwd: 'bower_components',      // Src matches are relative to this path.
                        src: [
                            'jquery/dist/jquery.min.js',
                            'socket.io-client/socket.io.js'
                        ], // Actual pattern(s) to match.
                        dest: 'build/static/js/3p'   // Destination path prefix.
                    }
                ]
            }
        },
        jshint: {
            files: ['Gruntfile.js', 'src/**/*.js' ],
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
        watch: {
            files: ['<%= jshint.files %>'],
            tasks: ['copy:build', 'jshint']
        }
    });

    grunt.registerTask('test', ['jshint']);

    grunt.registerTask('default', ['clean', 'jshint', 'copy:build' ]);

};
