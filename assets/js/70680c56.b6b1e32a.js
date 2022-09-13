"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[513],{349:function(t,e,a){a.r(e),a.d(e,{assets:function(){return m},contentTitle:function(){return d},default:function(){return k},frontMatter:function(){return o},metadata:function(){return p},toc:function(){return u}});var n=a(7462),r=a(3366),l=(a(7294),a(3905)),i=(a(8766),a(5710),["components"]),o={slug:"/usage/configuration",sidebar_position:1,title:"Connection Configuration"},d="SQLiter",p={unversionedId:"usage/configuration",id:"usage/configuration",title:"Connection Configuration",description:"Connection Configuration",source:"@site/docs/usage/configuration.mdx",sourceDirName:"usage",slug:"/usage/configuration",permalink:"/SQLiter/usage/configuration",draft:!1,editUrl:"https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/docs/usage/configuration.mdx",tags:[],version:"current",sidebarPosition:1,frontMatter:{slug:"/usage/configuration",sidebar_position:1,title:"Connection Configuration"},sidebar:"mainSidebar",previous:{title:"Usage",permalink:"/SQLiter/usage"},next:{title:"Database Migrations",permalink:"/SQLiter/usage/migrations"}},m={},u=[{value:"Connection Configuration",id:"connection-configuration",level:2},{value:"Required parameters",id:"required-parameters",level:2},{value:"Optional parameters",id:"optional-parameters",level:2},{value:"Extended configuration",id:"extended-configuration",level:3},{value:"Logging",id:"logging",level:3},{value:"Lifecycle",id:"lifecycle",level:3},{value:"Encryption",id:"encryption",level:3}],g={toc:u};function k(t){var e=t.components,a=(0,r.Z)(t,i);return(0,l.kt)("wrapper",(0,n.Z)({},g,a,{components:e,mdxType:"MDXLayout"}),(0,l.kt)("h1",{id:"sqliter"},"SQLiter"),(0,l.kt)("h2",{id:"connection-configuration"},"Connection Configuration"),(0,l.kt)("p",null,"The database configuration used to open a connection supplies a number of generally ",(0,l.kt)("em",{parentName:"p"},"sane")," defaults, but there are a\nquite a number of properties that could be overridden."),(0,l.kt)("h2",{id:"required-parameters"},"Required parameters"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"name")),(0,l.kt)("td",{parentName:"tr",align:null},"String?"),(0,l.kt)("td",{parentName:"tr",align:null},"The database filename to open, or the name to assign to an in-memory database")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"version")),(0,l.kt)("td",{parentName:"tr",align:null},"Int"),(0,l.kt)("td",{parentName:"tr",align:null},"The expected database schema version")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"create")),(0,l.kt)("td",{parentName:"tr",align:null},"(DatabaseConnection) -> Unit"),(0,l.kt)("td",{parentName:"tr",align:null},"Lambda to call if the database schema needs to be created")))),(0,l.kt)("h2",{id:"optional-parameters"},"Optional parameters"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"upgrade")),(0,l.kt)("td",{parentName:"tr",align:null},"(DatabaseConnection, Int, Int) -> Unit"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to doing nothing when versions of the database don\u2019t match.  Supply your own upgrade lambda for other migration strategies.")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"inMemory")),(0,l.kt)("td",{parentName:"tr",align:null},"Boolean"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"false"),".  Set to ",(0,l.kt)("inlineCode",{parentName:"td"},"true")," if you need an in-memory database")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"journalMode")),(0,l.kt)("td",{parentName:"tr",align:null},"JournalMode"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"JournalMode.WAL"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"extendedConfig")),(0,l.kt)("td",{parentName:"tr",align:null},"Extended"),(0,l.kt)("td",{parentName:"tr",align:null},"See below")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"loggingConfig")),(0,l.kt)("td",{parentName:"tr",align:null},"Logging"),(0,l.kt)("td",{parentName:"tr",align:null},"See below")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"lifecycleConfig")),(0,l.kt)("td",{parentName:"tr",align:null},"Lifecycle"),(0,l.kt)("td",{parentName:"tr",align:null},"See below")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"encryptionConfig")),(0,l.kt)("td",{parentName:"tr",align:null},"Encryption"),(0,l.kt)("td",{parentName:"tr",align:null},"See below")))),(0,l.kt)("h3",{id:"extended-configuration"},"Extended configuration"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"foreignKeyConstraints")),(0,l.kt)("td",{parentName:"tr",align:null},"Boolean"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"false"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"busyTimeout")),(0,l.kt)("td",{parentName:"tr",align:null},"Int"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to 5000")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"pageSize")),(0,l.kt)("td",{parentName:"tr",align:null},"Int?"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"null"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"basePath")),(0,l.kt)("td",{parentName:"tr",align:null},"String?"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"null"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"synchronousFlag")),(0,l.kt)("td",{parentName:"tr",align:null},"SynchronousFlag?"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"null"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"recursiveTriggers")),(0,l.kt)("td",{parentName:"tr",align:null},"Boolean"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"false"))),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"lookasideSlotSize")),(0,l.kt)("td",{parentName:"tr",align:null},"Int"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to -1")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"lookasideSlotCount")),(0,l.kt)("td",{parentName:"tr",align:null},"Int"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to -1")))),(0,l.kt)("h3",{id:"logging"},"Logging"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"logger")),(0,l.kt)("td",{parentName:"tr",align:null},"Logger"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"WarningLogger")," (errors are enabled, verbose logging not)")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"verboseDataCalls")),(0,l.kt)("td",{parentName:"tr",align:null},"Boolean"),(0,l.kt)("td",{parentName:"tr",align:null},"Defaults to ",(0,l.kt)("inlineCode",{parentName:"td"},"false"),". SQLiter will verbose log execution of calls in the sqlite statement if this is enabled.")))),(0,l.kt)("h3",{id:"lifecycle"},"Lifecycle"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"onCreateConnection")),(0,l.kt)("td",{parentName:"tr",align:null},"(DatabaseConnection) -> Unit"),(0,l.kt)("td",{parentName:"tr",align:null},"Called when the connection is opened, but ",(0,l.kt)("strong",{parentName:"td"},"before")," version/migration checking")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"onCloseConnection")),(0,l.kt)("td",{parentName:"tr",align:null},"(DatabaseConnection) -> Unit"),(0,l.kt)("td",{parentName:"tr",align:null},"Called ",(0,l.kt)("strong",{parentName:"td"},"after")," the sqlite connection is closed")))),(0,l.kt)("h3",{id:"encryption"},"Encryption"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"Name"),(0,l.kt)("th",{parentName:"tr",align:null},"Type"),(0,l.kt)("th",{parentName:"tr",align:null},"Description"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"key")),(0,l.kt)("td",{parentName:"tr",align:null},"String?"),(0,l.kt)("td",{parentName:"tr",align:null},"Used for creating encrypted databases or accessing an existing encrypted database.")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("strong",{parentName:"td"},"rekey")),(0,l.kt)("td",{parentName:"tr",align:null},"String?"),(0,l.kt)("td",{parentName:"tr",align:null},"Used to encrypt an existing unencrypted database, change the encryption key of an existing encrypted database or remove encryption from an existing encrypted database.")))))}k.isMDXComponent=!0}}]);