'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
    uploadImageBody,
    genNewUser,
    genNewUserReply,
    genNewHouse,
    genNewHouseReply,
    genNewRental,
    genNewRentalReply,
}


const {faker} = require('@faker-js/faker');
const fs = require('fs')

const locations = [
  'Lisbon',
  'Porto',
  'Faro',
  'Sobreda',
  'Tavira'
];

var imagesIds = []
var images = []
var users = []
var houses = []
var rentals = []

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [ ["/rest/media/","GET"],
			["/rest/media","POST"],
		    ["/rest/user/","GET"],
		    ["/rest/house/","POST"],
		    ["/rest/house/","GET"],
		    ["/rest/rental/","POST"],
		    ["/rest/rental/","GET"],
	]

// Function used to compress statistics
global.myProcessEndpoint = function( str, method) {
	var i = 0;
	for( i = 0; i < statsPrefix.length; i++) {
		if( str.startsWith( statsPrefix[i][0]) && method == statsPrefix[i][1])
			return method + ":" + statsPrefix[i][0];
	}
	return method + ":" + str;
}

// Auxiliary function to select an element from an array
Array.prototype.sample = function(){
	   return this[Math.floor(Math.random()*this.length)]
}

// Returns a random value, from 0 to val
function random( val){
	return Math.floor(Math.random() * val)
}

// Loads data about images from disk
function loadData() {
    var i
    var basefile = "images/house."
    // if( fs.existsSync( '/images')) 
    // 	basefile = '/images/house.'
    // else
    // 	basefile =  'images/cats.'	
    for( i = 1; i <= 40 ; i++) {
	var img  = fs.readFileSync(basefile + i + '.jpg')
	images.push( img)
    }
    var str;

    if( fs.existsSync('users.data')) {
	str = fs.readFileSync('users.data','utf8')
	users = JSON.parse(str)
    }

    if( fs.existsSync('houses.data')) {
	str = fs.readFileSync('houses.data','utf8')
	houses = JSON.parse(str)
    }

        if( fs.existsSync('rentals.data')) {
	str = fs.readFileSync('rentals.data','utf8')
	rentals = JSON.parse(str)
    }
}

loadData();

/**
 * Sets the body to an image, when using images.
 */
function uploadImageBody(requestParams, context, ee, next) {
	requestParams.body = images.sample()
	return next()
}

/**
 * Process reply of the download of an image. 
 * Update the next image to read.
 */
function processUploadReply(requestParams, response, context, ee, next) {
	if( typeof response.body !== 'undefined' && response.body.length > 0) {
		imagesIds.push(response.body)
	}
    return next()
}

/**
 * Select an image to download.
 */
function selectImageToDownload(context, events, done) {
	if( imagesIds.length > 0) {
		context.vars.imageId = imagesIds.sample()
	} else {
		delete context.vars.imageId
	}
	return done()
}

/**
 * Select an user to download.
 */
function selectUser(context, events, done) {
	if( userIds.length > 0) {
		context.vars.userId = userIds.sample()
	} else {
		delete context.vars.userId
	}
	return done()
}

/**
 * Generate data for a new user using Faker
 */
function genNewUser(context, events, done) {
    //const first = `${Faker.name.firstName()}`
    // const last = `${Faker.name.lastName()}`
    context.vars.id = faker.internet.userName()
    context.vars.name = faker.internet.displayName() //first + " " + last
    context.vars.pwd = `${faker.internet.password()}`
    return done()
}


/**
 * Process reply for of new users to store the id on file
 */
function genNewUserReply(requestParams, response, context, ee, next) {
	if( response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0)  {
		let u = JSON.parse( response.body)
		users.push(u)
		fs.writeFileSync('users.data', JSON.stringify(users));
	}
    return next()
}


function genNewHouse(context, events, done) {
    context.vars.id= faker.string.uuid()
    context.vars.ownerId= users.sample().id
    context.vars.name= faker.location.streetAddress()
    context.vars.location= faker.location.city() // todo usar locations array
    context.vars.description= faker.lorem.sentence()
    context.vars.photoIds= [faker.string.uuid(), faker.string.uuid(), faker.string.uuid()]
    context.vars.normalPrice= faker.number.float({ min: 1000, max: 5000, precision: 0.01 })
    context.vars.promotionPrice= faker.number.float({ min: 800, max: 1000, precision: 0.01 })
    context.vars.monthWithDiscount= faker.number.int({ min: 1, max: 12 })
    return done()
}

function genNewHouseReply(requestParams, response, context, ee, next) {
	if( response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0)  {
		let h = JSON.parse( response.body)
		houses.push(h)
		fs.writeFileSync('houses.data', JSON.stringify(houses));
	}
    return next()
}


function genNewRental(context, events, done) {
    const house = houses.sample()
    context.vars.id = faker.string.uuid();
    context.vars.houseId = house.id;
    context.vars.userId = users.sample().id; // Assuming users array is available
    context.vars.day = faker.date.future({years: 1});
    context.vars.price = house.normalPrice;

    return done();
}

function genNewRentalReply(requestParams, response, context, ee, next) {
	if( response.statusCode >= 200 && response.statusCode < 300 && response.body.length > 0)  {
		let r = JSON.parse( response.body)
		rentals.push(r)
		fs.writeFileSync('rental.data', JSON.stringify(houses));
	}
    return next()
}


