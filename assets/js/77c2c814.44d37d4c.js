"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[29],{447:function(e,t,a){a.r(t),a.d(t,{assets:function(){return l},contentTitle:function(){return d},default:function(){return u},frontMatter:function(){return r},metadata:function(){return c},toc:function(){return p}});var n=a(7462),i=a(3366),o=(a(7294),a(3905)),s=(a(8766),a(5710),["components"]),r={slug:"/usage/migrations",sidebar_position:2,title:"Database Migrations"},d="SQLiter",c={unversionedId:"usage/migrations",id:"usage/migrations",title:"Database Migrations",description:"Database version migrations",source:"@site/docs/usage/migrations.mdx",sourceDirName:"usage",slug:"/usage/migrations",permalink:"/SQLiter/usage/migrations",draft:!1,editUrl:"https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/docs/usage/migrations.mdx",tags:[],version:"current",sidebarPosition:2,frontMatter:{slug:"/usage/migrations",sidebar_position:2,title:"Database Migrations"},sidebar:"mainSidebar",previous:{title:"Connection Configuration",permalink:"/SQLiter/usage/configuration"},next:{title:"Encrypted Databases",permalink:"/SQLiter/usage/encrypted"}},l={},p=[{value:"Database version migrations",id:"database-version-migrations",level:2}],m={toc:p};function u(e){var t=e.components,a=(0,i.Z)(e,s);return(0,o.kt)("wrapper",(0,n.Z)({},m,a,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("h1",{id:"sqliter"},"SQLiter"),(0,o.kt)("h2",{id:"database-version-migrations"},"Database version migrations"),(0,o.kt)("p",null,"Sqliter will check the version of the database that you open, and trigger a call to either the ",(0,o.kt)("em",{parentName:"p"},"create")," or the ",(0,o.kt)("em",{parentName:"p"},"upgrade")," lambdas\ndepending on what is read from the database."),(0,o.kt)("p",null,"It is the responsibility of the consumer to determine and execute the upgrade strategy given the connection and old/new version numbers,\nand the responsibility of SQLiter to provide the trigger that a ",(0,o.kt)("em",{parentName:"p"},"create")," or ",(0,o.kt)("em",{parentName:"p"},"upgrade")," should occur."),(0,o.kt)("p",null,(0,o.kt)("strong",{parentName:"p"},"Note:")," the ",(0,o.kt)("em",{parentName:"p"},"Lifecycle")," ",(0,o.kt)("inlineCode",{parentName:"p"},"onCloseConnection")," callback will be called ",(0,o.kt)("strong",{parentName:"p"},"AFTER")," the sqlite connection is closed, and the\ncorresponding ",(0,o.kt)("inlineCode",{parentName:"p"},"onCreateConnection")," will be called as soon as the connection is made, but ",(0,o.kt)("strong",{parentName:"p"},"BEFORE")," any migrations have\nhappened.  These are called regardless of the version checking that happens."),(0,o.kt)("div",{className:"admonition admonition-info alert alert--info"},(0,o.kt)("div",{parentName:"div",className:"admonition-heading"},(0,o.kt)("h5",{parentName:"div"},(0,o.kt)("span",{parentName:"h5",className:"admonition-icon"},(0,o.kt)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"14",height:"16",viewBox:"0 0 14 16"},(0,o.kt)("path",{parentName:"svg",fillRule:"evenodd",d:"M7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-3.14 2.56-5.7 5.7-5.7zM7 1C3.14 1 0 4.14 0 8s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7zm1 3H6v5h2V4zm0 6H6v2h2v-2z"}))),"Opening a database with no version check")),(0,o.kt)("div",{parentName:"div",className:"admonition-content"},(0,o.kt)("p",{parentName:"div"},"The database manager will skip version checks, create, and update when the requested version is set to\n",(0,o.kt)("inlineCode",{parentName:"p"},"NO_VERSION_CHECK"),". This is useful if you need to do some kind of operation on a database without initializing it.\nFor example, converting from clear text to an encrypted database.  Using this value is a bit of a hack. The next\nmajor version will likely include a refactor of config to provide a cleaner mechanism to skip version checking."))))}u.isMDXComponent=!0}}]);