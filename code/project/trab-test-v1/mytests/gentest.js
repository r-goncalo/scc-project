const {faker} = require('@faker-js/faker');
const fs = require('fs');
const path = require('path');

// Function to generate random user data
function generateUserData() {
    return {
        id: faker.internet.userName(),
        name: faker.internet.displayName(),
        pwd: faker.internet.password(),
        photoId: null,
    };
}

// Function to generate random house data
function generateHouseData(ownerId) {
    return {
        id: faker.string.uuid(),
        ownerId: ownerId,
        name: faker.location.streetAddress(),
        location: faker.location.city(),
        description: faker.lorem.sentence(),
        photoIds: [faker.string.uuid(), faker.string.uuid(), faker.string.uuid()]
    };
}

// Generate a user
const user = generateUserData();
const renter = generateUserData();

const userLogin =genLogin(user);
const renterLogin = genLogin(renter)

function genLogin(user) {
    return {
        user: user.id,
        pwd: user.pwd,
    };
}

// Generate an array of house objects with the user as the owner
const houses = Array.from({ length: 10 }, () => generateHouseData(user.id));



const userDirectory = path.join(__dirname, "users");
fs.mkdirSync(userDirectory);

// Write user data to a file
fs.writeFileSync(path.join(userDirectory, 'user.json'), JSON.stringify(user, null, 2));
fs.writeFileSync(path.join(userDirectory, 'userLogin.json'), JSON.stringify(userLogin, null, 2));

// Write renter data to a file
fs.writeFileSync(path.join(userDirectory, 'renter.json'), JSON.stringify(renter, null, 2));
fs.writeFileSync(path.join(userDirectory, 'renterLogin.json'), JSON.stringify(renterLogin, null, 2));

const houseDirectory = path.join(__dirname, "houses");
fs.mkdirSync(houseDirectory);

// Write each house data to a separate file
houses.forEach((house, index) => {
    const houseFileName = `house_${index + 1}.json`;
    fs.writeFileSync(path.join(houseDirectory, houseFileName), JSON.stringify(house, null, 2));
});

console.log('Files written successfully.');
