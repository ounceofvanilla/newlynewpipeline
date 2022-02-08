theList = []
test_no = 0
err_no = 0
the_tests = []

with open('kw_results.out') as f:
        theList = f.readlines()
        theList = [x.strip() for x in theList]

for x in theList:
        if x == '<problem>':
                test_no+=1
                the_tests.append([])
        if '<file>' in x:
                the_tests[test_no-1].append(x)
                the_tests[test_no-1][0] = the_tests[test_no-1][0].replace('<file>','').replace('</file>','')

        if '<line>' in x:
                the_tests[test_no-1].append(x)
                the_tests[test_no-1][1] = the_tests[test_no-1][1].replace('<line>','').replace('</line>','')

        if '<code>' in x:
                the_tests[test_no-1].append(x)
                the_tests[test_no-1][2] = the_tests[test_no-1][2].replace('<code>','').replace('</code>','')

        if '<message>' in x:
                the_tests[test_no-1].append(x)
                the_tests[test_no-1][3] = the_tests[test_no-1][3].replace('<message>','').replace('</message>','').replace('&apos;','"')

        if '<severity>' in x:
                the_tests[test_no-1].append(x)
                the_tests[test_no-1][4] = the_tests[test_no-1][4].replace('<severity>','').replace('</severity>','')
                if  the_tests[test_no-1][4] == 'Error':
                    err_no+=1
  
xml_template = '''<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
   <testsuite name="JUnitTest" tests="{tests}" failures="{tests}" errors="{errors}">
'''.format(tests=test_no,errors=err_no)

for x in the_tests:
        xml_template+='      <testcase classname="'+x[2]+'" name="LINE_'+x[1]+'">\n'
        xml_template+='         <failure type="'+x[4]+'">Fault found in:'+x[0]+' | Message: '+x[3]+'</failure>\n'
        xml_template+='      </testcase>\n'

xml_template+='''   </testsuite>
</testsuites>'''
print(xml_template)

text_file = open("kw_output.xml", "w")
text_file.write(xml_template)
text_file.close()