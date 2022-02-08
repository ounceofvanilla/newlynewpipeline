import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
  	  name="devopspkg",
      version="0.0.0",
      author="DevopsFactory",
      author_email="Do-Not-Reply@harris.com",
      description="Devops Pipeline using SetupTools",
      long_description=long_description,
      long_description_content_type="text/markdown",
      url="https://lnsvr0329.gcsd.harris.com:8443/bitbucket",
      packages=['src'],
      license='N/A',
      classifiers=[
        "Programming Language :: Python :: 3",
      ],
)