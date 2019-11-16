#include <iostream>
#include "omp.h"
#include <stdlib.h> // srand, rand
#include <time.h>   // time
#include <math.h>
#include <bits/stdc++.h>
#include <w32api/sysinfoapi.h>

using namespace std;

void swap(int &from, int &to) {
    int tmp = from;
    from = to;
    to = tmp;
}

void trickleDown(int arr[], int lastPos, int parent) {
    int left = 2 * parent + 1;
    int right = 2 * parent + 2;
    int largest = parent;

    if (left <= lastPos && arr[left] > arr[largest]) {
        largest = left;
    }

    if (right <= lastPos && arr[right] > arr[largest]) {
        largest = right;
    }

    if (largest != parent) {
        swap(arr[parent], arr[largest]);
        trickleDown(arr, lastPos, largest);
    }
}

void trickleUp(int arr[], int lastPos, int position) {
    if (position == 0)
        return;

    int parent = (int) floor((position - 1) / 2);

    if (arr[position] > arr[parent]) {
        swap(arr[position], arr[parent]);
        trickleUp(arr, lastPos, parent);
    }
}

void remove(int arr[], int &lastPos) {
    swap(arr[0], arr[lastPos--]);
    trickleDown(arr, lastPos, 0);
}

void add(int arr[], int newElement) {
    static int i = -1;
    arr[++i] = newElement;
    //trickleUp(arr, i, i);
}

/* A utility function to print array of size n */
void printArray(int arr[], int n) {
    for (int i = 0; i < n; ++i)
        cout << arr[i] << " ";
    cout << "\n";
}

int calculateMidVal(int arr[], int arrLength) {
    int arrAv = 0;

    for (int i = 0; i < arrLength; i++)
        arrAv += arr[i];

    return arrAv / arrLength;
}

/*void swapArrays(int leftArr[], int rightArr[], int leftArrLength, int rightArrLength) {
    int midVal = calculateMidVal(leftArr, rightArr, leftArrLength, rightArrLength);

    for (int i = 0, j = rightArrLength - 1; i < leftArrLength && j >= 0;) {
        if (leftArr[i] <= midVal && ++i >= leftArrLength) return;

        if (rightArr[j] > midVal && --j < 0) return;

        if (leftArr[i] > midVal && rightArr[j] <= midVal) {
            swap(leftArr[i], rightArr[j]);
            i++;
            j--;
        }
    }

    for (int i = 0, j = rightArrLength - 1; i < leftArrLength;) {
        if (leftArr[i] <= midVal) {
            i++;
            continue;
        }

        if (rightArr[j] < leftArr[i]) {
            swap(leftArr[i], rightArr[j]);
            i++;
            j--;
        } else
            j--;
    }
}*/

int swapArrays2(int arr[], int arrLength) {
    int pivotPoint = arrLength - 1;
    int value = arr[pivotPoint];
    int counter = 0;

    for (int i = 0; i < pivotPoint; i++) {
        if (arr[i] <= value) {
            swap(arr[i], arr[counter]);
            counter++;
        }
    }
    swap(arr[counter], arr[pivotPoint]);

    return counter;
}

void heapSortParallel(int arr[], int arrLength, int maxLengthOfPartition) {
    if (arrLength <= 1)
        return;

    int midLocation = swapArrays2(arr, arrLength);
    //swapArrays(leftArr, rightArr, leftArrLength, rightArrLength);

    int leftArrLength = midLocation - 0;
    int rightArrLength = arrLength - midLocation - 1;
    int *leftArr = arr;
    int *rightArr = leftArr + leftArrLength + 1;

    cout << "Left: ";
    printArray(leftArr, leftArrLength);
    cout << "Right: ";
    printArray(rightArr, rightArrLength);

    if (leftArrLength > maxLengthOfPartition)
        heapSortParallel(leftArr, leftArrLength, maxLengthOfPartition);
    else if (leftArrLength > 1) {
        int lastPos = -1;

        for (int i = 0; i < leftArrLength; i++) {
            lastPos++;
            trickleUp(leftArr, lastPos, lastPos);
        }

        for (int i = 0; i < leftArrLength - 1; i++) {
            remove(leftArr, lastPos);
        }

        cout << "After trickle left: ";
        printArray(leftArr, leftArrLength);

        copy(leftArr, leftArr + leftArrLength, arr);
    }

    if (rightArrLength > maxLengthOfPartition)
        heapSortParallel(rightArr, rightArrLength, maxLengthOfPartition);
    else if (rightArrLength > 1) {
        int lastPos = -1;

        for (int i = 0; i < rightArrLength; i++) {
            lastPos++;
            trickleUp(rightArr, lastPos, lastPos);
        }

        for (int i = 0; i < rightArrLength - 1; i++) {
            remove(rightArr, lastPos);
        }

        cout << "After trickle right: ";
        printArray(rightArr, rightArrLength);

        copy(rightArr, rightArr + rightArrLength, arr + leftArrLength);
    }
}

int main() {
    int arrLength = 0;

    cout << "Enter count of elements in array: ";
    cin >> arrLength;

    int *arr = new int[arrLength];

    cout << endl;

    for (int i = 0; i < arrLength; i++) {
        arr[i] = 0; //clear array for beautiful output
    }

    srand(time(nullptr));

    double start = omp_get_wtime();

    for (int i = 0; i < arrLength; i++) {
        int randomNumber = 1 + (rand() % (arrLength - 1 + 1));
        add(arr, randomNumber);
    }

    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    int numCPU = systemInfo.dwNumberOfProcessors;

    double stop = omp_get_wtime();

    double execTime = stop - start;

    cout << "In array: " << endl;
    printArray(arr, arrLength);
    cout << endl;
    cout << "Num of threads = " << numCPU << endl << endl;

    start = omp_get_wtime();

    //omp_set_num_threads(numCPU);

    heapSortParallel(arr, arrLength, arrLength / numCPU);

    stop = omp_get_wtime();

    cout << endl;
    cout << "Execution time  = " << execTime + (stop - start) << endl;
    cout << "Result:" << endl;
    printArray(arr, arrLength);
}
