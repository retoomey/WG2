void main(void)
 {
     //gl_FragColor = vec4(0.0, 1.0, 1.0, 1.0);
     vec4 test = gl_Color;
     test.x = 0;
     gl_FragColor = test;
 }