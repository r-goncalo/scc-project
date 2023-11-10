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
        photoIds: [faker.string.uuid(), faker.string.uuid(), faker.string.uuid()],
        normalPrice: faker.number.float({ min: 1000, max: 5000, precision: 0.01 }),
        promotionPrice: faker.number.float({ min: 800, max: 1000, precision: 0.01 }),
        monthWithDiscount: faker.number.int({ min: 1, max: 12 }),
    };
}

// Generate a user
const user = generateUserData();
const renter = generateUserData();

// Generate an array of house objects with the user as the owner
const houses = Array.from({ length: 10 }, () => generateHouseData(user.id));



const userDirectory = path.join(__dirname, "users");
fs.mkdirSync(userDirectory);

// Write user data to a file
fs.writeFileSync(path.join(userDirectory, 'user.json'), JSON.stringify(user, null, 2));

// Write renter data to a file
fs.writeFileSync(path.join(userDirectory, 'renter.json'), JSON.stringify(renter, null, 2));

const houseDirectory = path.join(__dirname, "houses");
fs.mkdirSync(houseDirectory);

// Write each house data to a separate file
houses.forEach((house, index) => {
    const houseFileName = `house_${index + 1}.json`;
    fs.writeFileSync(path.join(houseDirectory, houseFileName), JSON.stringify(house, null, 2));
});

console.log('Files written successfully.');
