from os import path
import setuptools

pwd = path.abspath(path.dirname(__file__))
rdm_file = path.join(pwd, 'README.md')
with open(rdm_file, encoding='utf-8') as f:
    readme = f.read()

setuptools.setup(
    name='vertx-eventbus-client',
    version='1.0.0.dev0',
    packages=['vertx'],
    author='vertx-dev',
    author_email='vertx-dev@googlegroups.com',
    url='https://github.com/vert-x3/vertx-eventbus-bridge-clients/tree/master/python',
    license='Apache Software License 2.0',
    description='Vertx EventBus Client',
    long_description=readme,
    long_description_content_type='text/markdown'
)
