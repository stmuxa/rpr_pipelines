
String property1 = 'pipeline_tools property value'
static String static_property1 = 'pipeline_tools static_property1 value'

def call(String param)
{
    echo "pipeline_tools Constructor param = ${param}"
    return this
}

def method1()
{
    echo "pipeline_tools method1"
}

static def static_method1()
{
    echo "pipeline_tools static_method1"
}
